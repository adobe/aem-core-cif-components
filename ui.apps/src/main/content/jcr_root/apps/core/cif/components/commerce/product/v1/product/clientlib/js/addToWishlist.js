/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
'use strict';

/**
 * Add to wishlist button component.
 */

class AddToWishlist {
    constructor(config) {
        this._element = config.element;
        let parent_sku = config.product.querySelector(AddToWishlist.selectors.sku).innerHTML;
        let grouped = config.product.dataset.grouped !== undefined;
        let productId = config.product.id;

        this._state = {
            parent_sku,
            grouped,
            productId
        };

        // Add click handler to add to wishlist button
        this._element.addEventListener('click', this._onAddToWishlist.bind(this));
    }

    /**
     * Click event handler for add to whislist button.
     */
    _onAddToWishlist(event) {
        const target = event.currentTarget;
        const items = this._getEventDetail();
        if (items.length > 0 && window.CIF) {
            const customEvent = new CustomEvent(AddToWishlist.events.addToWishlist, {
                bubbles: true,
                detail: items
            });
            target.dispatchEvent(customEvent);
            event.preventDefault();
            event.stopPropagation();
        }
    }

    _getEventDetail() {
        // To support grouped products where multiple products can be put in the wishlist in one single click,
        // the sku of each product is now read from the 'data-product-sku' attribute of each select element
        const selections = Array.from(document.querySelectorAll(AddToWishlist.selectors.quantity)).filter(selection => {
            return parseInt(selection.value) > 0;
        });

        if (this._state.grouped) {
            return [
                {
                    productId: this._state.productId,
                    sku: this._state.parent_sku,
                    quantity: 1
                }
            ];
        } else {
            return selections.map(selection => {
                const item = {
                    productId: selection.dataset.productId,
                    sku: selection.dataset.productSku,
                    quantity: selection.value
                };

                if (item.sku != this._state.parent_sku) {
                    item.parent_sku = this._state.parent_sku;
                }

                return item;
            });
        }
    }
}

AddToWishlist.selectors = {
    self: '.productFullDetail__cartActions button[data-cmp-is=add-to-wish-list]',
    sku: '.productFullDetail__details [role=sku]',
    quantity: '.productFullDetail__quantity select',
    product: '[data-cmp-is=product]'
};

AddToWishlist.events = {
    addToWishlist: 'aem.cif.add-to-wishlist'
};

(function(document) {
    function onDocumentReady() {
        // Initialize AddToCart component
        const productCmp = document.querySelector(AddToWishlist.selectors.product);
        const addToWishlistCmp = document.querySelector(AddToWishlist.selectors.self);
        if (addToWishlistCmp) new AddToWishlist({ element: addToWishlistCmp, product: productCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default AddToWishlist;
