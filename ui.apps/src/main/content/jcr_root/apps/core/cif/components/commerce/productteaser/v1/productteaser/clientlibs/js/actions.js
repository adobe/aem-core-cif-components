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
    constructor(element) {
        this.virtual = element.dataset.virtual !== undefined;
        this.loadPrices = element.dataset.loadPrice;

        const actionButtons = element.querySelectorAll(`.productteaser__cta button`);
        actionButtons.forEach(actionButton => {
            const action = actionButton.dataset['action'];
            let actionHandler;
            switch (action) {
                case 'addToCart':
                    actionHandler = this._addToCartHandler.bind(this);
                    break;
                case 'details':
                    actionHandler = this._seeDetailsHandler;
                    break;
                case 'wishlist':
                    actionHandler = this._addToWishlistHandler.bind(this);
                    break;
                default:
                    actionHandler = this._noOpHandler;
            }

            actionButton.addEventListener('click', ev => {
                const element = ev.currentTarget;
                actionHandler(element.dataset);
            });
        });

        this.loadPricess && this._fetchPrices();
    }

    _noOpHandler() {
        /* As the name says... NOOP */
    }

    _addToCartHandler(dataset) {
        const sku = dataset['itemSku'];
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [{ sku, quantity: 1, virtual: this.virtual }]
        });
        document.dispatchEvent(customEvent);
    }

    _addToWishlistHandler(dataset) {
        const sku = dataset['itemSku'];
        const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
            detail: [{ sku, quantity: 1 }]
        });
        document.dispatchEvent(customEvent);
    }

    _seeDetailsHandler(dataset) {
        const url = dataset['url'];
        const target = dataset['target'];
        if (target) {
            LocationAdapter.openHref(url, target);
        } else {
            LocationAdapter.setHref(url);
        }
    }

    async _fetchPrices() {}
}

ProductTeaser.selectors = {
    rootElement: '[data-cmp-is=productteaser]'
};

export { LocationAdapter };
export default ProductTeaser;

(function(doc) {
    function onDocumentReady() {
        const rootElements = doc.querySelectorAll(ProductTeaser.selectors.rootElement);
        rootElements.forEach(element => new ProductTeaser(element));
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
