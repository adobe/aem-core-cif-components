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

let productListCtx = (function(document) {
    'use strict';

    class ProductList {
        constructor(config) {
            this._element = config.element;

            // Local state
            this._state = {
                // List of skus currently displayed in the list
                skus: [],

                // Map with client-side fetched prices
                prices: {},

                // Load prices on the client-side
                loadPrices: this._element.dataset.loadClientPrice !== undefined
            };

            // Intl.NumberFormat instance for formatting prices
            this._formatter =
                window.CIF && window.CIF.PriceFormatter && new window.CIF.PriceFormatter(this._element.dataset.locale);

            this._element.querySelectorAll(ProductList.selectors.item).forEach(item => {
                this._state.skus.push(item.dataset.sku);
            });

            this._state.loadPrices && this._fetchPrices();
        }

        _fetchPrices() {
            // Retrieve current prices
            if (!window.CIF || !window.CIF.CommerceGraphqlApi) return;
            return window.CIF.CommerceGraphqlApi.getProductPrices(this._state.skus, false)
                .then(prices => {
                    this._state.prices = prices;

                    // Update prices
                    this._updatePrices();
                })
                .catch(err => {
                    console.error('Could not fetch prices', err);
                });
        }

        _updatePrices() {
            this._element.querySelectorAll(ProductList.selectors.item).forEach(item => {
                if (!(item.dataset.sku in this._state.prices)) return;
                item.querySelector('.item__price [role=price]').innerText = this._formatter.formatPrice(
                    this._state.prices[item.dataset.sku]
                );
            });
        }
    }

    ProductList.selectors = {
        self: '[data-cmp-is=productlist]',
        price: '.item__price [role=price]',
        item: '.item__root[role=product]'
    };

    function onDocumentReady() {
        // Initialize product list component
        const productListCmp = document.querySelector(ProductList.selectors.self);
        if (productListCmp) new ProductList({ element: productListCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }

    return {
        ProductList: ProductList,
        factory: config => {
            return new ProductList(config);
        }
    };
})(window.document);
