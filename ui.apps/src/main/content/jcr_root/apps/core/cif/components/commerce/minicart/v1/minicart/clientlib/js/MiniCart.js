/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

import tplTotals from './templates/carttotals.js';
import tplEmptyMiniCart from './templates/minicart-empty.js';
import tplBody from './templates/minicart-body.js';
import tplFooter from './templates/minicart-footer.js';
import tplEdit from './templates/minicart-edit.js';
import MiniCartItem from './MiniCartItem.js';
import Handlebars from 'handlebars';

/**
 * The object that drives the MiniCart component. It is responsible for fetching the data and rendering the cart items.
 */
(function() {
    'use strict';

    class MiniCart {
        constructor(props) {
            if (!props) {
                throw new Error('Missing props for the MiniCart library');
            }

            if (!props.commerceApi) {
                throw new Error('The CommerceApi was not supplied to the MiniCart library');
            }

            if (!props.pageContext) {
                throw new Error('The PageContext was not supplied to the MiniCart library');
            }

            if (!props.graphqlApi) {
                throw new Error('The grapqhql API was not supplied to the MiniCart library');
            }

            this.pageContext = props.pageContext;
            this.commerceApi = props.commerceApi;
            this.storage = props.storage;
            this.graphqlApi = props.graphqlApi;

            if (this.pageContext.cartInfo) {
                this.cartQuote = this.pageContext.cartInfo.cartQuote;
                this.cartId = this.pageContext.cartInfo.cartId;
            }

            this.rootNode = document.querySelector('.miniCart__root');

            this.totalsTemplate = Handlebars.compile(tplTotals);
            this.emptyTemplate = Handlebars.compile(tplEmptyMiniCart);
            this.bodyTemplate = Handlebars.compile(tplBody);
            this.footerTemplate = Handlebars.compile(tplFooter);
            this.editTemplate = Handlebars.compile(tplEdit);

            this.removeItemHandler = this.removeItemHandler.bind(this);
            this.editHandler = this.editHandler.bind(this);

            this.items = [];
            this.state = { currentState: 'empty', previousState: 'empty' };
            this.init();
        }

        /**
         * Sets the state of the MiniCart component. The state can be one of the following:
         * empty - when the cart is empty
         * full - when the cart has some items in it
         * edit - when an item is in edit mode
         *
         * @param state
         * @returns {Promise<void>}
         */
        async setState(state) {
            this.state.previousState = this.state.currentState;
            this.state.currentState = state;
            if (state === 'empty') {
                this.renderEmpty();
            } else if (state === 'full') {
                await this.refreshItems();
                this.renderBody();
            } else if (state === 'edit') {
                this.renderEdit();
            }
        }

        /**
         * Initializes the shopping cart component
         * @returns {Promise<void>}
         */
        async init() {
            this._initializeBehavior();
            let response = await this.refreshData();

            // just trigger an event to let other components know we're ready.
            const event = new CustomEvent('aem.cif.cart-intialized', { detail: { quantity: this.cartQuantity } });
            document.dispatchEvent(event);
        }

        /*
         * Installs the event listeners for the MiniCart DOM
         */
        _initializeBehavior() {
            let closeButton = this.rootNode.querySelector("button[data-action='close']");
            closeButton.addEventListener('click', event => {
                this.close();
            });
        }

        /*
         *  Updates the data for to be displayed by the MiniCart
         */
        async refreshData() {
            if (!this.cartId || !this.cartQuote) {
                this.setState('empty');
            } else {
                let cartDataPromise = this.commerceApi.getCart(this.cartQuote);
                let cartTotalsPromise = this.commerceApi.getTotals(this.cartQuote);

                //issue the request for the cart items images as well

                return Promise.all([cartDataPromise, cartTotalsPromise]).then(async result => {
                    this.cartData = result[0];
                    this.cartTotals = result[1];

                    let productData = {};

                    this.cartData.items.forEach(item => {
                        let itemData = productData[item.name];
                        if (!itemData) {
                            productData[item.name] = [item.sku];
                        } else {
                            itemData.push(item.sku);
                        }
                    });

                    this.images = await this.graphqlApi.getProductImageUrls(productData);

                    if (this.cartData.items.length > 0) {
                        this.setState('full');
                    } else {
                        this.setState('empty');
                    }
                });
            }
        }

        /**
         * Builds the cart items based on the cart data.
         */
        refreshItems() {
            if (!this.cartData) {
                return;
            }
            let cartItems = this.cartData.items;
            this.items = [];

            const handlers = {
                removeItemHandler: this.removeItemHandler,
                editHandler: this.editHandler
            };

            let additionalData = { currency: this.cartData.currency.store_currency_code };

            cartItems.map(cartItem => {
                additionalData.imageUrl = this.images[cartItem.sku];
                this.items.push(new MiniCartItem(Object.assign({}, cartItem, additionalData), handlers));
            });
        }

        /**
         * Removes an item from the cart.
         * @param itemId the id of the cart item to remove
         * @returns {Promise<void>}
         */
        async removeItemHandler(itemId) {
            const success = await this.commerceApi.removeItem(this.cartQuote, itemId);
            await this.refreshData();

            let customEvent = new CustomEvent('aem.cif.product-removed-from-cart', {
                detail: { quantity: this.cartQuantity }
            });
            document.dispatchEvent(customEvent);
        }

        /**
         * Opens the edit side-panel for a cart item
         * @param itemId the id of the cart item.
         * @returns {Promise<void>}
         */
        async editHandler(itemId) {
            const miniCartItem = this.items.find(item => item.itemId === itemId);
            if (!miniCartItem) {
                return;
            }
            this.currentlyEditing = miniCartItem;
            this.setState('edit');
        }

        /**
         * Opens the MiniCart popup.
         */
        open() {
            this.rootNode.classList.add('miniCart__root_open');
            this.pageContext.maskPage();
        }

        /**
         * Closes the MiniCart popup
         */
        close() {
            this.rootNode.classList.remove('miniCart__root_open');
            this.pageContext.unmaskPage();
        }

        /**
         * Adds an entry to this cart.
         * @param args. An object in the shape of {sku, qty}
         */
        async addItem({ sku, qty }) {
            if (!this.cartQuote || !this.cartId) {
                // if we don't have a cart yet we have to create one, then add the item
                await this._createEmptyCart();
            }

            let params = {
                sku,
                qty,
                quoteId: this.cartQuote
            };

            let response = await this.commerceApi.postCartEntry(this.cartId, params);
            await this.refreshData();

            let customEvent = new CustomEvent('aem.cif.product-added-to-cart', {
                detail: { quantity: this.cartQuantity }
            });
            document.dispatchEvent(customEvent);

            this.open();
        }

        /**
         * Creates an empty shopping cart and sets the data in the page context.
         * @returns {Promise<void>}
         * @private
         */
        async _createEmptyCart() {
            let cartQuote = await window.CIF.CommerceApi.createCart();
            let cart = await window.CIF.CommerceApi.getCart(cartQuote);

            this.cartId = cart.id;
            this.cartQuote = cartQuote;

            let cartInfo = {};
            cartInfo.cartQuote = cartQuote;
            cartInfo.cartId = cart.id;
            window.CIF.PageContext.setCartInfoCookie(cartInfo);
        }

        get cartQuantity() {
            return this.cartTotals ? this.cartTotals.items_qty : 0;
        }

        /**
         * Empties the DOM element of the MiniCart component
         */
        emptyDom() {
            const elements = this.rootNode.children;
            while (this.rootNode.childElementCount > 1) {
                this.rootNode.removeChild(this.rootNode.lastChild);
            }
        }

        /**
         * Saves a cart item after being edited. This function only sets the quantity of the item in the cart.
         * @returns {Promise<void>}
         * @private
         */
        async _handleSaveItem() {
            let selectField = this.rootNode.querySelector('select[name="quantity"]');
            let itemData = this.currentlyEditing.itemData;
            let itemId = itemData.item_id;

            let newItemData = await this.commerceApi.updateCartEntry(this.cartId, itemId, {
                sku: itemData.sku,
                qty: selectField.value,
                quoteId: this.cartQuote
            });

            let response = await this.refreshData();

            let customEvent = new CustomEvent('aem.cif.product-cart-updated', {
                detail: { quantity: this.cartQuantity }
            });
            document.dispatchEvent(customEvent);
            this.setState('full');
        }

        /**
         * Renders the edit window DOM
         */
        renderEdit() {
            this.emptyDom();
            let html = this.editTemplate();

            this.rootNode.insertAdjacentHTML('beforeend', html);

            this.rootNode.querySelector('button[data-action="cancel"]').addEventListener('click', e => {
                this.setState('full');
            });

            this.rootNode.querySelector('button[data-action="save"').addEventListener('click', event => {
                this._handleSaveItem();
            });

            let qtySelectField = this.rootNode.querySelector(`select[name="quantity"]`);
            qtySelectField.selectedIndex = this.currentlyEditing.qty - 1;
        }

        /**
         * Renders an empty shopping cart.
         */
        renderEmpty() {
            this.emptyDom();
            let html = this.emptyTemplate();
            this.rootNode.insertAdjacentHTML('beforeend', html);
        }

        /**
         * Renders the body of the shopping cart with items and totals.
         */
        renderBody() {
            if (this.state.previousState !== this.state.currentState) {
                this.emptyDom();
                // recreate the sections from template and add them to the minicart
                let bodyHtml = this.bodyTemplate();
                this.rootNode.insertAdjacentHTML('beforeend', bodyHtml);
                let footerHtml = this.footerTemplate();
                this.rootNode.insertAdjacentHTML('beforeend', footerHtml);
            }

            // render the items, but empty the list first
            let itemListNode = this.rootNode.querySelector('ul.productList__root');
            while (itemListNode.firstChild) {
                itemListNode.removeChild(itemListNode.firstChild);
            }

            this.items.forEach((item, index) => {
                let li = document.createElement('li');
                item.renderTo(li);

                li.classList.add('product__root');
                li.dataset.index = index;
                li.dataset.key = item.sku;
                itemListNode.appendChild(li);
            });

            let miniCartBody = this.rootNode.querySelector('.miniCart__body');

            miniCartBody.addEventListener('click', e => {
                const dropdowns = miniCartBody.querySelectorAll('ul.kebab__dropdown_active');
                dropdowns.forEach(dd => {
                    dd.classList.remove('kebab__dropdown_active');
                });
            });

            //render the totals
            let totalsData = {
                quantity: this.cartTotals.items_qty,
                value: this.cartTotals.grand_total,
                currency: this.cartData.currency.store_currency_code
            };

            let html = this.totalsTemplate(totalsData);

            let totalsRoot = this.rootNode.querySelector("div[data-placeholder='totals']");
            while (totalsRoot.firstChild) {
                totalsRoot.removeChild(totalsRoot.firstChild);
            }
            totalsRoot.insertAdjacentHTML('beforeend', html);
        }
    }

    function onDocumentReady() {
        const pageContext = window.CIF.PageContext;
        const commerceApi = window.CIF.CommerceApi;
        const storage = window.CIF.Storage;
        const graphqlApi = window.CIF.CommerceGraphqlApi;
        createTestCart().then(res => {
            window.CIF.MiniCart = new MiniCart({ pageContext, commerceApi, storage, graphqlApi });
        });
    }

    async function createTestCart() {
        if (
            window.CIF.PageContext.cartInfo &&
            window.CIF.PageContext.cartInfo.cartId &&
            window.CIF.PageContext.cartInfo.cartQuote
        ) {
            return;
        }

        let cartQuote = await window.CIF.CommerceApi.createCart();
        let cart = await window.CIF.CommerceApi.getCart(cartQuote);

        let cartInfo = {};
        cartInfo.cartQuote = cartQuote;
        cartInfo.cartId = cart.id;
        window.CIF.PageContext.setCartInfoCookie(cartInfo);
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();
