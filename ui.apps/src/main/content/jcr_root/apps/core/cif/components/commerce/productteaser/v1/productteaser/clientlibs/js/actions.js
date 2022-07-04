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

    constructor(element, allBaseSkus, queryVariants) {
        this.element = element;
        this.sku = element.dataset.productSku;
        this.virtual = element.dataset.virtual !== undefined;
        this.loadPrices = window.CIF.enableClientSidePriceLoading;
        this._formatter = new window.CIF.PriceFormatter();

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

        this.loadPrices && this._fetchPrices(allBaseSkus || [this.sku], queryVariants);
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

    async _fetchPrices(allBaseSkus, queryVariants) {
        if (!ProductTeaser.prices$) {
            if (window.CIF.CommerceGraphqlApi) {
                ProductTeaser.prices$ = window.CIF.CommerceGraphqlApi.getProductPriceModels(allBaseSkus, queryVariants);
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
    const baseSkus = [];
    let queryVariants = false;
    for (let element of rootElements) {
        let { productBaseSku, productSku } = element.dataset;
        if (!productBaseSku) {
            productBaseSku = productSku;
        }
        // if any of the teasers base skus is different than the product sku, a variant is configured
        queryVariants = queryVariants || productBaseSku !== productSku;
        baseSkus.push(productBaseSku);
    }
    rootElements.forEach(element => new ProductTeaser(element, baseSkus, queryVariants));
}

export { LocationAdapter, onDocumentReady };
export default ProductTeaser;

(function(document) {
    if (window.CIF) {
        onDocumentReady(document);
    } else {
        document.addEventListener('aem.cif.clientlib-initialized', () => onDocumentReady(document));
    }
})(window.document);
