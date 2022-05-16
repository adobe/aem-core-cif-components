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
            actionButton.addEventListener('click', ev => {
                actionHandler(ev);
            });
        });

        element.querySelectorAll('.productcollection__item-button--add-to-wish-list').forEach(actionButton => {
            let actionHandler = this._addToWishlistHandler.bind(this);
            actionButton.addEventListener('click', ev => {
                ev.preventDefault();
                const element = ev.currentTarget;
                actionHandler(element.dataset);
            });
        });
    }

    _addToCartHandler(ev) {
        const dataset = ev.currentTarget.dataset;
        const sku = dataset['itemSku'];
        const action = dataset['action'];
        if (action === 'add-to-cart') {
            const customEvent = new CustomEvent('aem.cif.add-to-cart', {
                detail: [{ sku, quantity: 1, virtual: this.virtual }]
            });
            document.dispatchEvent(customEvent);
            ev.preventDefault();
        }
        // else click hits parent link
    }

    _addToWishlistHandler(dataset) {
        const sku = dataset['itemSku'];
        const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
            detail: [{ sku, quantity: 1 }]
        });
        document.dispatchEvent(customEvent);
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
