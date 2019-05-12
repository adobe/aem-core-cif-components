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
(function () {

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
            this.itemsListNode = this.miniCartBody.querySelector("ul");

            // just trigger an event to let other components know we're ready.
            const event = new CustomEvent("aem.cif.cart-intialized");
            document.dispatchEvent(event);

            this._initializeBehavior();

            // // if the pageContext contains some cart id, now it's the moment to retrieve the data
            // if (this.pageContext.cartId) {
            //     let cartData = commerceApi.getCart(pagetContext.cartId);
            // }

            // inject dummy data
            this.items = [];

            const handlers = {
                removeItemHandler: this.removeItemHandler,
                addToFavesHandler: this.addToFavesHandler,
                editHandler:this.editHandler
            };

            const item = new MiniCartItem({}, handlers);
            this.items.push(item);
            this.items.push(new MiniCartItem({}, handlers));
        }

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

        removeItemHandler = (index) => {
            console.log(`Removing item`);
        };

        addToFavesHandler = () => {
            console.log(`Adding to faves`, this);
        };

        editHandler = () => {
            console.log(`Editing...`);
        };

        open() {
            this.rootNode.classList.add("miniCart__root_open");
            this.pageContext.maskPage();
        }

        close() {
            this.rootNode.classList.remove("miniCart__root_open");
            this.pageContext.unmaskPage();
        }

        emptyItemsList() {
            while (this.itemsListNode.firstChild) {
                this.itemsListNode.removeChild(this.itemsListNode.firstChild);
            }
        }

        render() {

            this.emptyItemsList();
            this.items.forEach((item, index) => {
                let li = document.createElement("li");
                item.renderTo(li);

                li.classList.add("product__root");
                li.dataset.index = index;
                //todo set the "key" property to the actual SKU of the product
                li.dataset.key='itemsku';
                this.itemsListNode.appendChild(li);
            })
        }

    }

    function onDocumentReady() {
        const pageContext = window.CIF.PageContext;
        const commerceApi = window.CIF.CommerceApi;

        window.CIF.MiniCart = new MiniCart({pageContext, commerceApi});

        window.CIF.MiniCart.render();
    }

    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})();