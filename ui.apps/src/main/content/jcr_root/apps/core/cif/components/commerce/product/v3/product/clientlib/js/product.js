/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

import DOMPurify from 'dompurify';

class Product {
    constructor(config) {
        this._element = config.element;

        // Local state
        const sku = config.element.dataset.productSku ? config.element.dataset.productSku : null;
        this._state = {
            // Current sku, either from the base product or from a variant
            sku,

            // True if this product is configurable and has variants
            configurable: this._element.dataset.configurable !== undefined,

            // Map with client-side fetched prices
            prices: {},

            // Load prices on the client-side
            loadPrices: !!(sku && window.CIF.enableClientSidePriceLoading)
        };

        // Intl.NumberFormat instance for formatting prices
        this._formatter = new window.CIF.PriceFormatter(window.CIF.locale);

        // Update product data
        this._element.addEventListener(Product.events.variantChanged, this._onUpdateVariant.bind(this));

        this._state.loadPrices && this._initPrices();
    }

    _initPrices() {
        // Retrieve current prices
        if (!window.CIF.CommerceGraphqlApi) return;
        return window.CIF.CommerceGraphqlApi.getProductPriceModels([this._state.sku], true)
            .then(convertedPrices => {
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
                    // Update base product price
                    this._updatePrice(this._state.prices[this._state.sku]);
                    this._updateJsonLdPrice(this._state.prices);
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
        [this._element.querySelector(Product.selectors.price), this._element].forEach(
            element => element && element.setAttribute('data-product-sku', variant.sku)
        );
        // Update values and enable add to cart button
        const skuEl = this._element.querySelector(Product.selectors.sku);
        if (skuEl) skuEl.innerText = variant.sku;
        const nameEl = this._element.querySelector(Product.selectors.name);
        if (nameEl) nameEl.innerText = variant.name;
        const descriptionEl = this._element.querySelector(Product.selectors.description);
        if (descriptionEl) descriptionEl.innerHTML = DOMPurify.sanitize(variant.description);

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
        // Only update if prices are not null
        if (!price || !price.regularPrice || !price.finalPrice) {
            return;
        }

        const sku = optionalSku || this._state.sku;
        const innerHTML = this._formatter.formatPriceAsHtml(price, {
            showDiscountPercentage: true
        });
        const priceEl = this._element.querySelector(Product.selectors.price + `[data-product-sku="${sku}"]`);
        if (priceEl) priceEl.innerHTML = innerHTML;
    }

    _updateJsonLdPrice(prices) {
        if (!window.CIF.enableClientSidePriceLoading || !document.querySelector('script[type="application/ld+json"]')) {
            return;
        }

        const jsonLdScript = document.querySelector('script[type="application/ld+json"]');
        const jsonLdData = JSON.parse(jsonLdScript.innerHTML.trim());

        if (Array.isArray(jsonLdData.offers)) {
            let priceUpdated = false;

            jsonLdData.offers.forEach(offer => {
                const convertedPrice = prices[offer.sku];
                if (convertedPrice) {
                    offer.price = convertedPrice.finalPrice;
                    if (offer.priceSpecification) {
                        offer.priceSpecification.price = convertedPrice.regularPrice;
                    }
                    priceUpdated = true;
                }
            });

            if (priceUpdated) {
                const sanitizedJson = DOMPurify.sanitize(
                    JSON.stringify(jsonLdData, null, 2)
                        .replace(/},\s*{/g, '},\n{')
                        .replace(/\[\s*{/g, '[\n{')
                        .replace(/}\s*\]/g, '}\n]')
                );
                jsonLdScript.textContent = sanitizedJson;
            }
        }
    }
}
Product.selectors = {
    self: '[data-cmp-is=product]',
    sku: '.productFullDetail__sku [role=sku]',
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
        const productCmps = document.querySelectorAll(Product.selectors.self);
        for (let productCmp of productCmps) {
            new Product({ element: productCmp });
        }
    }

    const documentReady =
        document.readyState !== 'loading'
            ? Promise.resolve()
            : new Promise(r => document.addEventListener('DOMContentLoaded', r));
    const cifReady = window.CIF
        ? Promise.resolve()
        : new Promise(r => document.addEventListener('aem.cif.clientlib-initialized', r));

    Promise.all([documentReady, cifReady]).then(onDocumentReady);
})(window.document);

export default Product;
