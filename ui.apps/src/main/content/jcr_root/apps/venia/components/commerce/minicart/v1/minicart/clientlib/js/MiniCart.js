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

'use strict';

(function (templates) {

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

            this.pageContext = props.pageContext;
            this.commerceApi = props.commerceApi;

            if (this.pageContext.cartInfo) {
                this.cartQuote = this.pageContext.cartInfo.cartQuote;
                this.cartId = this.pageContext.cartInfo.cartId;
            }

            this.rootNode = document.querySelector(".miniCart__root");

            this.totalsTemplate = Handlebars.compile(templates.totals);
            this.emptyTemplate = Handlebars.compile(templates.emptyMiniCart);
            this.bodyTemplate = Handlebars.compile(templates.body);
            this.footerTemplate = Handlebars.compile(templates.footer);

            this.removeItemHandler = this.removeItemHandler.bind(this);
            this.items = [];
            this.state = {currentState: 'empty', previousState: 'empty'};
            this.init();
        }

        async setState(state) {
            this.state.previousState = this.state.currentState;
            this.state.currentState = state;
            console.log(`Setting component state to ${state}`);
            if (state === 'empty') {
                this.renderEmpty();
            } else if (state === 'full') {
                await this.refreshItems();
                this.renderBody();
            } else if (state === 'edit') {
                this.renderEditItem();
            }

        }

        async init() {
            this._initializeBehavior();
            await this.refreshData();

            // just trigger an event to let other components know we're ready.
            const event = new CustomEvent("aem.cif.cart-intialized", {detail: {quantity: this.cartQuantity}});
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
         * Initializes the data for to be displayed by the MiniCart
         */
        async refreshData() {
            if (!this.cartId || !this.cartQuote) {
                console.log(`No cart information present, nothing to do`);
                this.setState('empty');
            } else {

                this.cartData = await this.commerceApi.getCart(this.cartQuote);
                this.cartTotals = await this.commerceApi.getTotals(this.cartQuote);

                if (this.cartData.items.length > 0) {
                    this.setState('full');
                } else {
                    this.setState('empty');
                }
            }
        }


        refreshItems() {
            if (!this.cartData) {
                return;
            }
            let cartItems = this.cartData.items;
            this.items = [];

            const handlers = {
                removeItemHandler: this.removeItemHandler,
                addToFavesHandler: this.addToFavesHandler,
                editHandler: this.editHandler
            };

            let moneyData = {currency: this.cartData.currency.store_currency_code};

            cartItems.map(cartItem => this.items.push(new MiniCartItem(Object.assign({}, cartItem, moneyData), handlers)));
        }


        async removeItemHandler(itemId) {
            console.log(`Removing item ${itemId}`);
            const success = await this.commerceApi.removeItem(this.cartQuote, itemId);
            await this.refreshData();

            let customEvent = new CustomEvent("aem.cif.product-removed-from-cart", {detail: {quantity: this.cartQuantity}});
            document.dispatchEvent(customEvent);
        };

        editHandler(index) {
            console.log(`Editing...`);
        };

        /**
         * Opens the MiniCart popup.
         */
        open() {
            this.rootNode.classList.add("miniCart__root_open");
            this.pageContext.maskPage();
        }

        /**
         * Closes the MiniCart popup
         */
        close() {
            this.rootNode.classList.remove("miniCart__root_open");
            this.pageContext.unmaskPage();
        }

        /**
         * Adds an entry to this cart.
         * @param args. An object in the shape of {sku, qty}
         */
        async addItem(args) {

            if (!this.cartQuote || !this.cartId) {
                // if we don't have a cart yet we have to create one, then add the item
                await this._createEmptyCart();
            }

            args.quoteId = this.cartQuote;
            let response = await this.commerceApi.postCartEntry(this.cartId, args);
            console.log(response);
            await this.refreshData();

            let customEvent = new CustomEvent("aem.cif.product-added-to-cart", {detail: {quantity: this.cartQuantity}});
            document.dispatchEvent(customEvent);

            this.open();
        };

        async _createEmptyCart() {
            let cartQuote = await window.CIF.CommerceApi.createCart();
            let cart = await window.CIF.CommerceApi.getCart(cartQuote);

            this.cartId = cart.id;
            this.cartQuote = cartQuote;

            let cartInfo = {};
            cartInfo.cartQuote = cartQuote;
            cartInfo.cartId = cart.id;
            window.CIF.PageContext.setCartInfo(cartInfo);

        }

        get cartQuantity() {
            return this.cartTotals ? this.cartTotals.items_qty : 0;
        }

        emptyDom() {

            const elements = this.rootNode.children;

            while(this.rootNode.childElementCount > 1) {
                this.rootNode.removeChild(this.rootNode.lastChild);
            }

        }

        renderEmpty() {
            console.log(`Rendering empty cart..`);
            this.emptyDom();
            let html = this.emptyTemplate();
            this.rootNode.insertAdjacentHTML('beforeend', html);
        }

        renderBody() {
            console.log(`Rendering the body...`);
            if (this.state.previousState !== this.state.currentState) {
                this.emptyDom();
                console.log(`Recreating the DOM...`);
                // recreate the sections from template and add them to the minicart
                let bodyHtml = this.bodyTemplate();
                this.rootNode.insertAdjacentHTML('beforeend', bodyHtml);
                let footerHtml = this.footerTemplate();
                this.rootNode.insertAdjacentHTML('beforeend', footerHtml);

            }

            // render the items, but empty the list first
            let itemListNode = this.rootNode.querySelector("ul.productList__root");
            while (itemListNode.firstChild) {
                itemListNode.removeChild(itemListNode.firstChild);
            }

            this.items.forEach((item, index) => {
                let li = document.createElement("li");
                item.renderTo(li);

                li.classList.add("product__root");
                li.dataset.index = index;
                li.dataset.key = item.sku;
                itemListNode.appendChild(li);
            });

            let miniCartBody = this.rootNode.querySelector(".miniCart__body");

            miniCartBody.addEventListener("click", e => {
                const dropdowns = miniCartBody.querySelectorAll("ul.kebab__dropdown_active");
                dropdowns.forEach(dd => {
                    dd.classList.remove("kebab__dropdown_active");
                })
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
        createTestCart().then(res => {
            window.CIF.MiniCart = new MiniCart({pageContext, commerceApi});
        });
    }

    async function createTestCart() {
        if (window.CIF.PageContext.cartInfo && window.CIF.PageContext.cartInfo.cartId && window.CIF.PageContext.cartInfo.cartQuote) {
            return;
        }

        let cartQuote = await window.CIF.CommerceApi.createCart();
        let cart = await window.CIF.CommerceApi.getCart(cartQuote);

        let cartInfo = {};
        cartInfo.cartQuote = cartQuote;
        cartInfo.cartId = cart.id;
        window.CIF.PageContext.setCartInfo(cartInfo);
    }


    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})(window.CIF.MiniCart.templates);