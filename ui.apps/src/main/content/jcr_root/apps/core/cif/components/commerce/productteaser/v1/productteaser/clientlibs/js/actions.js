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

const LocationAdapter = {
    setHref(url) {
        window.location.assign(url);
    },

    openHref(url, target) {
        window.open(url, target) || window.location.assign(url);
    }
};

class ProductTeaser {
    static prices$ = null;

    constructor(element, allBaseSkus) {
        this.element = element;
        this.sku = element.dataset.productSku;
        this.allBaseSkus = allBaseSkus || [this.sku];
        this.virtual = element.dataset.virtual !== undefined;
        this.loadPrices = element.dataset.loadPrice !== undefined;
        this._formatter =
            window.CIF && window.CIF.PriceFormatter && new window.CIF.PriceFormatter(element.dataset.locale);

        const actionButtons = element.querySelectorAll(`.productteaser__cta button`);
        actionButtons.forEach(actionButton => {
            const action = actionButton.dataset['action'];
            let actionHandler;
            switch (action) {
                case 'addToCart':
                    actionHandler = this._addToCartHandler;
                    break;
                case 'details':
                    actionHandler = this._seeDetailsHandler;
                    break;
                case 'wishlist':
                    actionHandler = this._addToWishlistHandler;
                    break;
                default:
                    // noop
                    return;
            }

            actionButton.addEventListener('click', e => {
                actionHandler.call(this, e.target.dataset);
            });
        });

        this.loadPrices && this._fetchPrices();
    }

    _addToCartHandler(dataset) {
        const sku = dataset.itemSku;
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [{ sku, quantity: 1, virtual: this.virtual }]
        });
        document.dispatchEvent(customEvent);
    }

    _addToWishlistHandler(dataset) {
        const sku = dataset.itemSku;
        const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
            detail: [{ sku, quantity: 1 }]
        });
        document.dispatchEvent(customEvent);
    }

    _seeDetailsHandler(dataset) {
        const url = dataset.url;
        const target = dataset.target;
        if (target) {
            LocationAdapter.openHref(url, target);
        } else {
            LocationAdapter.setHref(url);
        }
    }

    async _fetchPrices() {
        if (!ProductTeaser.prices$) {
            if (window.CIF && window.CIF.CommerceGraphqlApi) {
                ProductTeaser.prices$ = window.CIF.CommerceGraphqlApi.getProductPriceModels(this.allBaseSkus, true);
            } else {
                ProductTeaser.prices$ = Promise.reject(new Error('CommerceGraphqlApi unavailable'));
            }
        }

        // await all prices to be loaded and update
        this._updatePrices(await ProductTeaser.prices$);
    }

    _updatePrices(prices) {
        if (!(this.sku in prices)) return;
        const price = prices[this.sku];

        // Only update if prices are available and not null
        if (!price || !price.regularPrice || !price.finalPrice) {
            return;
        }

        const innerHTML = this._formatter.formatPriceAsHtml(price);
        this.element.querySelector(ProductTeaser.selectors.priceElement).innerHTML = innerHTML;
    }
}

ProductTeaser.selectors = {
    rootElement: '[data-cmp-is=productteaser]',
    priceElement: '.price'
};

function onDocumentReady(document) {
    const rootElements = [...document.querySelectorAll(ProductTeaser.selectors.rootElement)];
    const baseSkus = rootElements
        .map(teaser => teaser.dataset.productBaseSku || teaser.dataset.productSku)
        .filter(sku => !!sku)
        .filter((value, index, array) => array.indexOf(value) === index);
    rootElements.forEach(element => new ProductTeaser(element, baseSkus));
}

export { LocationAdapter, onDocumentReady };
export default ProductTeaser;

(function(document) {
    const documentReady =
        document.readyState !== 'loading'
            ? Promise.resolve()
            : new Promise(r => document.addEventListener('DOMContentLoaded', r));
    const cifReady = window.CIF
        ? Promise.resolve()
        : new Promise(r => document.addEventListener('aem.cif.clientlib-initialized', r));

    Promise.all([documentReady, cifReady]).then(() => onDocumentReady(document));
})(window.document);
