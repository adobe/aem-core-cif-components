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

            this.rootNode = document.querySelector(".miniCart__root");
            this.miniCartBodyNode = this.rootNode.querySelector(".miniCart__body");
            this.miniCartFooterNode = this.rootNode.querySelector(".miniCart__footer");
            this.itemsListNode = this.miniCartBodyNode.querySelector("ul");

            this.totalRootNode = this.miniCartFooterNode.querySelector("div[data-placeholder='totals']");
            this.totalsTemplate = Handlebars.compile(templates.totals);

            this._initializeBehavior();
            this._initializeData();

            this.removeItemHandler = this.removeItemHandler.bind(this);
        }

        /*
         * Installs the event listeners for the MiniCart DOM
         */
        _initializeBehavior() {
            let closeButton = this.rootNode.querySelector("button[data-action='close']");
            closeButton.addEventListener('click', event => {
                this.close();
            });

            this.miniCartBodyNode.addEventListener("click", e => {
                const dropdowns = this.miniCartBodyNode.querySelectorAll("ul.kebab__dropdown_active");
                dropdowns.forEach(dd => {
                    dd.classList.remove("kebab__dropdown_active");
                })
            })
        }

        /*
         * Initializes the data for to be displayed by the MiniCart
         */
        async _initializeData() {
            // inject dummy data
            //if the pageContext contains some cart id, now it's the moment to retrieve the data
            if (!this.pageContext.cartInfo) {
                return;
            }

            this.cartQuote = this.pageContext.cartInfo.cartQuote;
            this.cartId = this.pageContext.cartInfo.cartId;
            await this.refreshItems();
            // just trigger an event to let other components know we're ready.
            const event = new CustomEvent("aem.cif.cart-intialized",{detail: { quantity: this.cartQuantity}});
            document.dispatchEvent(event);
        }

        async refreshItems() {

            this.items = [];
            this.cartData = await this.commerceApi.getCart(this.cartQuote);
            this.cartTotals = await this.commerceApi.getTotals(this.cartQuote);

            let cartItems = this.cartData.items;

            const handlers = {
                removeItemHandler: this.removeItemHandler,
                addToFavesHandler: this.addToFavesHandler,
                editHandler: this.editHandler
            };

            let moneyData = {currency: this.cartData.currency.store_currency_code};

            cartItems.map(cartItem => this.items.push(new MiniCartItem(Object.assign({}, cartItem, moneyData), handlers)));
            this.render();
        }

        async removeItemHandler(itemId) {
            console.log(`Removing item ${itemId}`);

            const success = await this.commerceApi.removeItem(this.cartQuote, itemId);
            await this.refreshItems();

            let customEvent = new CustomEvent("aem.cif.product-removed-from-cart", {detail: {quantity: this.cartQuantity}});
            document.dispatchEvent(customEvent);
        };

        addToFavesHandler(index) {
            console.log(`Adding to faves`, this);
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
            args.quoteId = this.cartQuote;
            let response = await this.commerceApi.postCartEntry(this.cartId, args)
            await this.refreshItems();

            let customEvent = new CustomEvent("aem.cif.product-added-to-cart", {detail: {quantity: this.cartQuantity}});
            document.dispatchEvent(customEvent);
        };

        get cartQuantity() {
            return this.cartTotals.items_qty;
        }

        render() {
            // render the cart body (i.e. the items)
            this._emptyDomNode(this.itemsListNode);
            this.items.forEach((item, index) => {
                let li = document.createElement("li");
                item.renderTo(li);

                li.classList.add("product__root");
                li.dataset.index = index;
                li.dataset.key = item.sku;
                this.itemsListNode.appendChild(li);
            });

            //render the totals

            this._emptyDomNode(this.totalRootNode);

            let totalsData = {
                quantity: this.cartTotals.items_qty,
                value: this.cartTotals.grand_total,
                currency: this.cartData.currency.store_currency_code
            };

            let html = this.totalsTemplate(totalsData);
            this._emptyDomNode(this.totalRootNode);

            this.miniCartFooterNode.querySelector("div[data-placeholder='totals']");

            /* Transforms a DOM string into an actual DOM Node object */
            const toElement = (domString) => {
                const html = new DOMParser().parseFromString(domString, "text/html");
                return html.body.firstChild;
            };

            this.totalRootNode.appendChild(toElement(html));
        }

        /**
         * Removes all the list elements from the list of items
         * @private
         */
        _emptyDomNode(node) {
            while (node.firstChild) {
                node.removeChild(node.firstChild);
            }
        }
    }


    function onDocumentReady() {
        const pageContext = window.CIF.PageContext;
        const commerceApi = window.CIF.CommerceApi;
        createTestCart();

        window.CIF.MiniCart = new MiniCart({pageContext, commerceApi});

    }

    async function createTestCart() {
        if (window.CIF.PageContext.cartInfo && window.CIF.PageContext.cartInfo.cartId && window.CIF.PageContext.cartInfo.cartQuote) {
            return;
        }

        let cartInfo = {};
        let cartQuote = await window.CIF.CommerceApi.createCart();
        let cart = await window.CIF.CommerceApi.getCart(cartQuote);

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