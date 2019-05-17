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

/**
 * A class describing a MiniCart item. It's responsible for rendering the item in the MiniCart component
 * @type {MiniCartItem}
 */
const MiniCartItem = (function (templates) {
    'use strict';

    class MiniCartItem {

        constructor(itemData, {removeItemHandler, editHandler}) {
            this.template = Handlebars.compile(templates.cartItem);

            this.removeItemHandler = removeItemHandler;
            this.editHandler = editHandler;
            this.itemData = itemData;
        }

        get sku() {
            return this.itemData.id;
        }

        get qty() {
            return this.itemData.qty;
        }

        get itemId() {
            return this.itemData.item_id;
        }

        get name() {
            return this.itemData.name;
        }

        renderTo(node) {

            if (!node) {
                return;
            }

            let html = this.template(this.itemData);

            // insert the rendered HTML into the DOM
            node.insertAdjacentHTML('beforeend', html);

            // install the drop-down trigger
            let trigger = node.querySelector(".kebab__root");
            let button = trigger.querySelector("button");
            let menu = trigger.querySelector("ul");
            button.addEventListener("click", e => {
                e.stopImmediatePropagation();
                menu.classList.toggle("kebab__dropdown_active");
            });

            this.mask = node.querySelector(".product__mask");

            // install the menu listeners
            node.querySelector("button[data-action='remove']").addEventListener("click", e => {
                const parent = e.target.parentElement;
                this.mask.style.visibility = "visible";
                this.removeItemHandler(this.itemId);
            });

            node.querySelector("button[data-action='edit']").addEventListener("click", e => {
                const parent = e.target.parentElement;
                this.editHandler(this.itemId);
            });


        }

    }

    return MiniCartItem;

})(window.CIF.MiniCart.templates);