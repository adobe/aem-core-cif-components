/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

class ProductCarouselActions {
    constructor(element) {
        this.virtual = element.dataset.virtual !== undefined;

        element.querySelectorAll('.product__card-button--add-to-cart').forEach(actionButton => {
            let actionHandler = this._addToCartHandler.bind(this);
            actionButton.addEventListener('click', ev => actionHandler(ev));
        });

        element.querySelectorAll('.product__card-button--add-to-wish-list').forEach(actionButton => {
            let actionHandler = this._addToWishlistHandler.bind(this);
            actionButton.addEventListener('click', ev => actionHandler(ev));
        });
    }

    _addToCartHandler(event) {
        const target = event.currentTarget;
        const dataset = target.dataset;
        const sku = dataset.itemSku;
        const action = dataset.action;

        const item = target.closest('.product__card');
        const itemDataLayer =
            (item.dataset.cmpDataLayer && Object.values(JSON.parse(item.dataset.cmpDataLayer))[0]) || {};
        const quantity = 1;
        const finalPrice = (itemDataLayer && itemDataLayer['xdm:listPrice']) || 0; // special price after discount
        const discountAmount = (itemDataLayer && itemDataLayer['xdm:discountAmount']) || 0;
        const regularPrice = finalPrice + discountAmount; // price before discount

        if (action === 'add-to-cart') {
            const customEvent = new CustomEvent('aem.cif.add-to-cart', {
                bubbles: true,
                detail: [
                    {
                        sku,
                        quantity,
                        virtual: this.virtual,
                        storefrontData: {
                            name: itemDataLayer['dc:title'] || sku,
                            regularPrice,
                            finalPrice,
                            currencyCode: itemDataLayer['xdm:currencyCode'] || ''
                        }
                    }
                ]
            });
            target.dispatchEvent(customEvent);
            event.preventDefault();
            event.stopPropagation();
        }
        // else click hits parent link
    }

    _addToWishlistHandler(event) {
        const target = event.currentTarget;
        const dataset = target.dataset;
        const sku = dataset.itemSku;
        const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
            bubbles: true,
            detail: [{ sku, quantity: 1 }]
        });
        target.dispatchEvent(customEvent);
        event.preventDefault();
        event.stopPropagation();
    }
}

ProductCarouselActions.selectors = {
    rootElement: '[data-comp-is=productcarousel]'
};

(function(doc) {
    function onDocumentReady() {
        const rootElements = doc.querySelectorAll(ProductCarouselActions.selectors.rootElement);
        rootElements.forEach(element => new ProductCarouselActions(element));
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default ProductCarouselActions;
