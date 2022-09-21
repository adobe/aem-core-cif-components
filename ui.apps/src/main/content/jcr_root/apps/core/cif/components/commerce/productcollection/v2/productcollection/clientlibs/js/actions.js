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

class ProductCollectionActions {
    constructor(element) {
        this.virtual = element.dataset.virtual !== undefined;

        element.querySelectorAll('.productcollection__item-button--add-to-cart').forEach(actionButton => {
            let actionHandler = this._addToCartHandler.bind(this);
            actionButton.addEventListener('click', ev => actionHandler(ev));
        });

        element.querySelectorAll('.productcollection__item-button--add-to-wish-list').forEach(actionButton => {
            let actionHandler = this._addToWishlistHandler.bind(this);
            actionButton.addEventListener('click', ev => actionHandler(ev));
        });
    }

    _addToCartHandler(event) {
        const target = event.currentTarget;
        const dataset = target.dataset;
        const sku = dataset.itemSku;
        const action = dataset.action;

        if (action === 'add-to-cart') {
            const quantity = 1;
            const detail = {
                sku,
                quantity,
                virtual: this.virtual
            };

            const item = target.closest('.productcollection__item');
            if (item.dataset.cmpDataLayer) {
                try {
                    const itemDataLayer = Object.values(JSON.parse(item.dataset.cmpDataLayer))[0];
                    const finalPrice = itemDataLayer['xdm:listPrice']; // special price after discount
                    const discountAmount = itemDataLayer['xdm:discountAmount'];
                    const regularPrice = finalPrice + discountAmount; // price before discount
                    detail.storefrontData = {
                        name: itemDataLayer['dc:title'],
                        regularPrice,
                        finalPrice,
                        currencyCode: itemDataLayer['xdm:currencyCode']
                    };
                } catch (e) {
                    // ignore
                }
            }

            const customEvent = new CustomEvent('aem.cif.add-to-cart', {
                bubbles: true,
                detail: [detail]
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

ProductCollectionActions.selectors = {
    rootElement: '[data-cmp-is=productcollection]'
};

(function(doc) {
    function onDocumentReady() {
        const rootElements = doc.querySelectorAll(ProductCollectionActions.selectors.rootElement);
        rootElements.forEach(element => new ProductCollectionActions(element));
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default ProductCollectionActions;
