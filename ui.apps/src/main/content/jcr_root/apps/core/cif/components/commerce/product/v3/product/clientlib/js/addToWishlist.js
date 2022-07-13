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
        let sku = config.product.dataset.productSku;
        let grouped = config.product.dataset.grouped !== undefined;
        let productId = config.product.id;

        this._state = {
            sku,
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
        const item = {
            productId: this._state.productId,
            sku: this._state.sku
        };

        let quantity = 1;

        if (!this._state.grouped) {
            const quantityEl = document.querySelector(AddToWishlist.selectors.quantity);
            if (quantityEl) {
                if (quantityEl.value == 0) return [];
                quantity = parseInt(quantityEl.value);
            }
        }

        item.quantity = quantity;

        return [item];
    }
}

AddToWishlist.selectors = {
    self: '[data-cmp-is=add-to-wish-list]',
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
