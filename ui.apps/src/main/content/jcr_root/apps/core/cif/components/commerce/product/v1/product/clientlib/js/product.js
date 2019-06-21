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

let productCtx = (function(document) {
    'use strict';

    class Product {
        constructor(config) {
            this._element = config.element;

            // Local state
            this._state = {
                // Current sku, either from the base product or from a variant
                sku: this._element.querySelector(Product.selectors.sku).innerHTML,

                // True if this product is configurable and has variants
                configurable: false,

                // Map with client-side fetched prices
                prices: {}
            };
            this._state.configurable = this._element.dataset.configurable !== undefined;

            // Update product data
            this._element.addEventListener(Product.events.variantChanged, this._onUpdateVariant.bind(this));

            // Retrieve current prices
            if (!window.CIF || !window.CIF.CommerceGraphqlApi) return;
            window.CIF.CommerceGraphqlApi.getProductPrice([this._state.sku])
                .then(prices => {
                    this._state.prices = prices;

                    // Update price
                    if (!(this._state.sku in prices)) return;
                    this._element.querySelector(Product.selectors.price).innerText = this._formatPrice(
                        prices[this._state.sku]
                    );
                })
                .catch(err => {
                    console.error('Could not fetch prices');
                });
        }

        _formatPrice(price) {
            return `${price.value} ${price.currency}`;
        }

        /**
         * Variant changed event handler that updates the displayed product attributes
         * based on the given event.
         */
        _onUpdateVariant(event) {
            const variant = event.detail.variant;
            if (!variant) return;

            // Update internal state
            this._state.sku = variant.sku;

            // Update values and enable add to cart button
            this._element.querySelector(Product.selectors.sku).innerText = variant.sku;
            this._element.querySelector(Product.selectors.name).innerText = variant.name;
            this._element.querySelector(Product.selectors.description).innerHTML = variant.description;

            // Use client-side fetched price
            if (this._state.sku in this._state.prices) {
                this._element.querySelector(Product.selectors.price).innerText = this._formatPrice(
                    this._state.prices[this._state.sku]
                );
            } else {
                // or server-side price as a backup
                this._element.querySelector(Product.selectors.price).innerText = variant.formattedPrice;
            }
        }
    }

    Product.selectors = {
        self: '[data-cmp-is=product]',
        sku: '.productFullDetail__details [role=sku]',
        name: '.productFullDetail__title [role=name]',
        price: '.productFullDetail__productPrice [role=price]',
        description: '.productFullDetail__description [role=description]',
        mainImage: '.carousel__currentImage'
    };

    Product.events = {
        variantChanged: 'variantchanged'
    };

    function onDocumentReady() {
        // Initialize product component
        const productCmp = document.querySelector(Product.selectors.self);
        if (productCmp) new Product({ element: productCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }

    return {
        Product: Product,
        factory: config => {
            return new Product(config);
        }
    };
})(window.document);
