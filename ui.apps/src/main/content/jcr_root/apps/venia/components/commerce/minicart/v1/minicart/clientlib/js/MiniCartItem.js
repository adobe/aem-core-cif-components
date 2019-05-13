/*******************************************************************************
 *
 *      Copyright 2019 Adobe. All rights reserved.
 *      This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License. You may obtain a copy
 *      of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software distributed under
 *      the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *      OF ANY KIND, either express or implied. See the License for the specific language
 *      governing permissions and limitations under the License.
 *
 *
 ******************************************************************************/
const MiniCartItem = (function (templates) {


    class MiniCartItem {

        constructor(itemData, {removeItemHandler, addToFavesHandler, editHandler}) {
            this.template = Handlebars.compile(templates.cartItem);

            this.removeItemHandler = removeItemHandler;
            this.addToFavesHandler = addToFavesHandler;
            this.editHandler = editHandler;
            this.itemData = itemData;
        }

        get sku() {
            return this.itemData.id;
        }

        get qty() {
            return this.itemData.qty;
        }

        renderTo(node) {

            if (!node) {
                return;
            }

            let html = this.template(this.itemData);

            /* Transforms a DOM string into an actual DOM Node object */
            const toElements = (domString) => {
                const html = new DOMParser().parseFromString(domString, "text/html");
                return html.body.querySelectorAll("div");
            };

            const installDropDownTrigger = (el) => {
                const button = el.querySelector("button");
                const menu = el.querySelector("ul");
                button.addEventListener("click", e => {
                    e.stopImmediatePropagation();
                    menu.classList.toggle("kebab__dropdown_active");
                });
            };

            toElements(html).forEach(el => {
                node.appendChild(el);
                if (el.classList.contains("kebab__root")) {
                    installDropDownTrigger(el);
                }
            });

            node.querySelector("button[data-action='favorite']").addEventListener("click", e => {
                this.addToFavesHandler();
            });

            node.querySelector("button[data-action='remove']").addEventListener("click", e => {
                const parent = e.target.parentElement;
                
            });

        }

    }

    return MiniCartItem;

})(window.CIF.MiniCart.templates);