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

let addToCartCtx = (function(document) {
    'use strict';

    /**
     * Add to cart button component.
     */
    class AddToCart {
        constructor(config) {
            this._element = config.element;

            // Get configuration from product reference
            let configurable = config.product.dataset.configurable !== undefined;
            let sku = !configurable ? config.product.querySelector(AddToCart.selectors.sku).innerHTML : null;

            this._state = {
                sku: sku,
                attributes: {},
                configurable: configurable
            };

            // Disable add to cart if configurable product and no variant was selected
            if (this._state.configurable && !this._state.sku) {
                this._element.disabled = true;
            }

            // Listen to variant updates on product
            config.product.addEventListener(AddToCart.events.variantChanged, this._onUpdateVariant.bind(this));

            // Add click handler to add to cart button
            this._element.addEventListener('click', this._onAddToCart.bind(this));
        }

        /**
         * Variant changed event handler.
         */
        _onUpdateVariant(event) {
            const variant = event.detail.variant;

            // Disable add to cart button if no valid variant is available
            if (!variant) {
                this._element.disabled = true;
                return;
            }

            // Update internal state
            this._state.sku = variant.sku;
            this._state.attributes = event.detail.attributes;
            this._element.disabled = false;
        }

        /**
         * Click event handler for add to cart button.
         */
        _onAddToCart() {
            const quantity = document.querySelector(AddToCart.selectors.quantity).value;

            if (this._state.sku) {
                console.log(
                    `Add product with sku ${this._state.sku} with quantity ${quantity} to cart.`,
                    this._state.attributes
                );
                return;
            }

            console.log('No variant selected that could be added to the cart.');
        }
    }

    AddToCart.selectors = {
        self: '.productFullDetail__cartActions button',
        sku: '.productFullDetail__details [role=sku]',
        quantity: '.productFullDetail__quantity select',
        product: '[data-cmp-is=product]'
    };

    AddToCart.events = {
        variantChanged: 'variantchanged'
    };

    function onDocumentReady() {
        // Initialize AddToCart component
        const productCmp = document.querySelector(AddToCart.selectors.product);
        const addToCartCmp = document.querySelector(AddToCart.selectors.self);
        if (addToCartCmp) new AddToCart({ element: addToCartCmp, product: productCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }

    return {
        AddToCart: AddToCart,
        factory: config => {
            return new AddToCart(config);
        }
    };
})(window.document);
