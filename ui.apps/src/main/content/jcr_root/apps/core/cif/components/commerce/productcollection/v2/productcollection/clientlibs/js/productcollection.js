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

const awaitCommonClientLib = async () =>
    new Promise(r => document.addEventListener('aem.cif.clientlib-initialized', r));

class ProductCollection {
    constructor(config) {
        this._element = config.element;

        let sortKeySelect = this._element.querySelector(ProductCollection.selectors.sortKey);
        if (sortKeySelect) {
            sortKeySelect.addEventListener('change', () => this._applySortKey(sortKeySelect));
        }

        let loadMoreButton = this._element.querySelector(ProductCollection.selectors.loadMoreButton);
        if (loadMoreButton) {
            loadMoreButton.addEventListener('click', () => this._loadMore(loadMoreButton));
        }

        let filters = this._element.querySelector(ProductCollection.selectors.filtersBody);
        if (filters) {
            let selectedFilter = null;
            filters.addEventListener('click', e => {
                if (e.target.type === 'radio') {
                    if (selectedFilter && selectedFilter === e.target) {
                        e.target.checked = false;
                        selectedFilter = null;
                    } else if (e.target.checked) {
                        selectedFilter = e.target;
                    }
                }
            });
        }

        // Local state
        this._state = {
            // List of skus currently displayed in the list
            skus: [],

            // Map with client-side fetched prices
            prices: {},

            // Load prices on the client-side
            loadPrices: this._element.dataset.loadClientPrice !== undefined
        };

        this._element.querySelectorAll(ProductCollection.selectors.item).forEach(item => {
            this._state.skus.push(item.dataset.sku);
        });

        this._state.loadPrices && this._fetchPrices();
    }

    /**
     * Convert given GraphQL PriceRange object into data structure as defined by the sling model.
     */
    _convertPriceToRange(range) {
        let price = {};
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

        price.isStartPrice = range.__typename == 'GroupedProduct';

        return price;
    }

    async _fetchPrices() {
        // Retrieve current prices
        if (!window.CIF || !window.CIF.CommerceGraphqlApi) {
            await awaitCommonClientLib();
        }

        try {
            const prices = await window.CIF.CommerceGraphqlApi.getProductPrices(this._state.skus, false);

            let convertedPrices = {};
            for (let key in prices) {
                convertedPrices[key] = this._convertPriceToRange(prices[key]);
            }
            this._state.prices = convertedPrices;

            // Update prices
            await this._updatePrices();
        } catch (err) {
            console.error('Could not fetch prices', err);
        }
    }

    async _updatePrices() {
        if (!this._formatter) {
            if (!window.CIF || !window.CIF.PriceFormatter) {
                await awaitCommonClientLib();
            }

            this._formatter = new window.CIF.PriceFormatter(this._element.dataset.locale);
        }

        let _formatter = this._formatter;

        for (let item of this._element.querySelectorAll(ProductCollection.selectors.item)) {
            if (!(item.dataset.sku in this._state.prices)) continue;

            const price = this._state.prices[item.dataset.sku];

            let innerHTML = '';
            if (!price.range) {
                if (price.discounted) {
                    innerHTML += `<span class="regularPrice">${_formatter.formatPrice({
                        value: price.regularPrice,
                        currency: price.currency
                    })}</span>
                        <span class="discountedPrice">${_formatter.formatPrice({
                            value: price.finalPrice,
                            currency: price.currency
                        })}</span>`;
                } else {
                    let prefix = price.isStartPrice ? _formatter.get('Starting at') + ' ' : '';
                    innerHTML += `<span>${prefix}${_formatter.formatPrice({
                        value: price.regularPrice,
                        currency: price.currency
                    })}</span>`;
                }
            } else {
                let from = _formatter.get('From');
                let to = _formatter.get('To');
                if (price.discounted) {
                    innerHTML += `<span class="regularPrice">${from} ${_formatter.formatPrice({
                        value: price.regularPrice,
                        currency: price.currency
                    })} ${to} ${_formatter.formatPrice({
                        value: price.regularPriceMax,
                        currency: price.currency
                    })}</span>
                        <span class="discountedPrice">${from} ${_formatter.formatPrice({
                        value: price.finalPrice,
                        currency: price.currency
                    })} ${to} ${_formatter.formatPrice({
                        value: price.finalPriceMax,
                        currency: price.currency
                    })}</span>`;
                } else {
                    innerHTML += `<span>${from} ${_formatter.formatPrice({
                        value: price.regularPrice,
                        currency: price.currency
                    })} ${to} ${_formatter.formatPrice({
                        value: price.regularPriceMax,
                        currency: price.currency
                    })}</span>`;
                }
            }

            item.querySelector(ProductCollection.selectors.price).innerHTML = innerHTML;
        }
    }

    _applySortKey(sortKeySelect) {
        window.location = sortKeySelect.options[sortKeySelect.selectedIndex].value;
    }

    async _fetchMoreProducts(loadMoreButton) {
        let url = loadMoreButton.dataset.loadMore;
        return fetch(url);
    }

    async _loadMore(loadMoreButton) {
        // Hide load more button and show spinner
        loadMoreButton.style.display = 'none';
        let spinner = this._element.querySelector(ProductCollection.selectors.loadMoreSpinner);
        spinner.style.display = 'block';

        let response = await this._fetchMoreProducts(loadMoreButton);
        if (!response.ok || response.errors) {
            // The query failed, we show the button again
            loadMoreButton.style.display = 'block';
            let message = await response.text();
            throw new Error(message);
        }

        // Delete load more button and hide spinner
        loadMoreButton.parentNode.removeChild(loadMoreButton);
        spinner.style.display = 'none';

        // Parse response and only select product items
        let text = await response.text();
        let domParser = new DOMParser();
        let more = domParser.parseFromString(text, 'text/html');
        let moreItems = more.querySelectorAll(ProductCollection.selectors.item);

        // Append new product items to existing product gallery
        let galleryItems = this._element.querySelector(ProductCollection.selectors.galleryItems);
        galleryItems.append(...moreItems);

        // If any, append the new "load more" button
        let newloadMoreButton = more.querySelector(ProductCollection.selectors.loadMoreButton);
        if (newloadMoreButton) {
            spinner.parentNode.insertBefore(newloadMoreButton, spinner);
            newloadMoreButton.addEventListener('click', () => this._loadMore(newloadMoreButton));
        }

        // Fetch prices
        if (this._state.loadPrices) {
            this._state.skus = Array.from(moreItems, item => item.dataset.sku);
            await this._fetchPrices();
        }
    }
}

ProductCollection.selectors = {
    self: '[data-cmp-is=productcollection]',
    price: '.price',
    item: '.productcollection__item[role=product]',
    sortKey: '.productcollection__sort-keys',
    galleryItems: '.productcollection__items',
    loadMoreButton: '.productcollection__loadmore-button',
    loadMoreSpinner: '.productcollection__loadmore-spinner',
    filtersBody: '.productcollection__filters-body'
};

(function(document) {
    function onDocumentReady() {
        // Initialize product collection component
        const productCollectionCmps = document.querySelectorAll(ProductCollection.selectors.self);
        for (let productCollectionCmp of productCollectionCmps) {
            new ProductCollection({ element: productCollectionCmp });
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

export default ProductCollection;
