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

class Product {
    constructor(config) {
        this._element = config.element;

        const skuElement = this._element.querySelector(Product.selectors.sku);
        // Local state
        this._state = {
            // Current sku, either from the base product or from a variant
            sku: skuElement && skuElement.innerHTML,

            // True if this product is configurable and has variants
            configurable: this._element.dataset.configurable !== undefined,

            // Map with client-side fetched prices
            prices: {},

            // Load prices on the client-side
            loadPrices: this._element.dataset.loadClientPrice !== undefined
        };

        // Intl.NumberFormat instance for formatting prices
        this._formatter =
            window.CIF && window.CIF.PriceFormatter && new window.CIF.PriceFormatter(this._element.dataset.locale);

        // Update product data
        this._element.addEventListener(Product.events.variantChanged, this._onUpdateVariant.bind(this));

        this._state.loadPrices && this._initPrices();
    }

    /**
     * Convert given GraphQL PriceRange object into data structure as defined by the sling model.
     */
    _convertPriceToRange(range) {
        let price = {};
        price.productType = range.__typename;
        price.currency = range.minimum_price.final_price.currency;
        price.regularPrice = range.minimum_price.regular_price.value;
        price.finalPrice = range.minimum_price.final_price.value;
        price.discountAmount = range.minimum_price.discount.amount_off;
        price.discountPercent = range.minimum_price.discount.percent_off;

        if (range.maximum_price) {
            price.regularPriceMax = range.maximum_price.regular_price.value;
            price.finalPriceMax = range.maximum_price.final_price.value;
            price.discountAmountMax = range.maximum_price.discount.amount_off;
            price.discountPercentMax = range.maximum_price.discount.percent_off;
        }

        price.discounted = !!(price.discountAmount && price.discountAmount > 0);
        price.range = !!(
            price.finalPrice &&
            price.finalPriceMax &&
            Math.round(price.finalPrice * 100) != Math.round(price.finalPriceMax * 100)
        );

        return price;
    }

    _initPrices() {
        // Retrieve current prices
        if (!window.CIF || !window.CIF.CommerceGraphqlApi) return;
        return window.CIF.CommerceGraphqlApi.getProductPrices([this._state.sku], true)
            .then(prices => {
                let convertedPrices = {};
                for (let key in prices) {
                    convertedPrices[key] = this._convertPriceToRange(prices[key]);
                }
                this._state.prices = convertedPrices;

                // Update price
                if (!(this._state.sku in this._state.prices)) return;
                if (this._state.prices[this._state.sku].productType == 'GroupedProduct') {
                    for (let key in this._state.prices) {
                        if (key == this._state.sku) {
                            continue; // Only update the prices of the items inside the group
                        }
                        this._updatePrice(this._state.prices[key], key);
                    }
                } else {
                    this._updatePrice(this._state.prices[this._state.sku]);
                }
            })
            .catch(err => {
                console.error('Could not fetch prices', err);
            });
    }

    /**
     * Variant changed event handler that updates the displayed product attributes
     * based on the given event.
     */
    _onUpdateVariant(event) {
        const variant = event.detail.variant;
        if (!variant) return;

        // Update internal state and 'data-product-sku' attribute of price element
        this._state.sku = variant.sku;
        this._element.querySelector(Product.selectors.price).setAttribute('data-product-sku', variant.sku);

        // Update values and enable add to cart button
        this._element.querySelector(Product.selectors.sku).innerText = variant.sku;
        this._element.querySelector(Product.selectors.name).innerText = variant.name;
        this._element.querySelector(Product.selectors.description).innerHTML = variant.description;

        // Use client-side fetched price
        if (this._state.sku in this._state.prices) {
            this._updatePrice(this._state.prices[this._state.sku]);
        } else {
            // or server-side price as a backup
            this._updatePrice(variant.priceRange);
        }
    }

    /**
     * Update price in the DOM.
     */
    _updatePrice(price, optionalSku) {
        let youSave = this._formatter.get('You save');
        let innerHTML = '';
        if (!price.range) {
            if (price.discounted) {
                innerHTML += `<span class="regularPrice">${this._formatter.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })}</span>
                    <span class="discountedPrice">${this._formatter.formatPrice({
                        value: price.finalPrice,
                        currency: price.currency
                    })}</span>
                    <span class="you-save">${youSave} ${this._formatter.formatPrice({
                    value: price.discountAmount,
                    currency: price.currency
                })} (${price.discountPercent}%)</span>`;
            } else {
                innerHTML += `<span>${this._formatter.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })}</span>`;
            }
        } else {
            let from = this._formatter.get('From');
            let to = this._formatter.get('To');
            if (price.discounted) {
                innerHTML += `<span class="regularPrice">${from} ${this._formatter.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })} ${to} ${this._formatter.formatPrice({
                    value: price.regularPriceMax,
                    currency: price.currency
                })}</span>
                    <span class="discountedPrice">${from} ${this._formatter.formatPrice({
                    value: price.finalPrice,
                    currency: price.currency
                })} ${to} ${this._formatter.formatPrice({
                    value: price.finalPriceMax,
                    currency: price.currency
                })}</span>
                    <span class="you-save">${youSave} ${this._formatter.formatPrice({
                    value: price.discountAmount,
                    currency: price.currency
                })} (${price.discountPercent}%)</span>`;
            } else {
                innerHTML += `<span>${from} ${this._formatter.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })} ${to} ${this._formatter.formatPrice({
                    value: price.regularPriceMax,
                    currency: price.currency
                })}</span>`;
            }
        }

        let sku = optionalSku || this._state.sku;
        this._element.querySelector(Product.selectors.price + `[data-product-sku="${sku}"]`).innerHTML = innerHTML;
    }
}

Product.selectors = {
    self: '[data-cmp-is=product]',
    sku: '.productFullDetail__details [role=sku]',
    name: '.productFullDetail__title [role=name]',
    price: '.price',
    description: '.productFullDetail__description [role=description]',
    mainImage: '.carousel__currentImage'
};

Product.events = {
    variantChanged: 'variantchanged'
};

(function(document) {
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
})(window.document);

export default Product;
