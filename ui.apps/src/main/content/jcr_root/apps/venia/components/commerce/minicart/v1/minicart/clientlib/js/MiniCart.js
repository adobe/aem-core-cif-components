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
            this.miniCartBody = this.rootNode.querySelector(".miniCart__body");
            this.miniCartFooter = this.rootNode.querySelector(".miniCart__footer");

            this.itemsListNode = this.miniCartBody.querySelector("ul");
            this.totalsTemplate = Handlebars.compile(templates.totals);

            // just trigger an event to let other components know we're ready.
            const event = new CustomEvent("aem.cif.cart-intialized");
            document.dispatchEvent(event);

            this._initializeBehavior();
            this._initializeData();
        }

        /*
         * Installs the event listeners for the MiniCart DOM
         */
        _initializeBehavior() {
            let closeButton = this.rootNode.querySelector("button[data-action='close']");
            closeButton.addEventListener('click', event => {
                this.close();
            });

            this.miniCartBody.addEventListener("click", e => {
                const dropdowns = this.miniCartBody.querySelectorAll("ul.kebab__dropdown_active");
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
            this.items = [];
            if (!this.pageContext.cartInfo) {
                return;
            }

            this.cartData = await this.commerceApi.getCart(this.pageContext.cartInfo.cartQuote);
            this.cartTotals = await this.commerceApi.getTotals(this.pageContext.cartInfo.cartQuote);

            let cartItems = this.cartData.items;

            console.log(this.items);
            const handlers = {
                removeItemHandler: this.removeItemHandler,
                addToFavesHandler: this.addToFavesHandler,
                editHandler: this.editHandler
            };

            let moneyData = {currency:this.cartData.currency.store_currency_code};

            cartItems.map(cartItem => this.items.push(new MiniCartItem(Object.assign({}, cartItem, moneyData), handlers)));
            this.render();

        }

        removeItemHandler(index) {
            console.log(`Removing item`);
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
        addItem(args) {
            this.commerceApi.postCartEntry(args).then(res => {
                console.log(res);
            })
        };

        render() {
            this._emptyItemListDom();
            this.items.forEach((item, index) => {
                let li = document.createElement("li");
                item.renderTo(li);

                li.classList.add("product__root");
                li.dataset.index = index;
                li.dataset.key = item.sku;
                this.itemsListNode.appendChild(li);
            })

            let totalsData = {
                quantity: this.cartTotals.items_qty,
                value:this.cartTotals.grand_total,
                currency:this.cartData.currency.store_currency_code
            };

            //render totals
            let html=this.totalsTemplate(totalsData);
            let totalsDomRoot = this.miniCartFooter.querySelector("div[data-placeholder='totals']");

            /* Transforms a DOM string into an actual DOM Node object */
            const toElement = (domString) => {
                const html = new DOMParser().parseFromString(domString, "text/html");
                return html.body.firstChild;
            };

            totalsDomRoot.appendChild(toElement(html));
        }

        /**
         * Removes all the list elements from the list of items
         * @private
         */
        _emptyItemListDom() {
            while (this.itemsListNode.firstChild) {
                this.itemsListNode.removeChild(this.itemsListNode.firstChild);
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