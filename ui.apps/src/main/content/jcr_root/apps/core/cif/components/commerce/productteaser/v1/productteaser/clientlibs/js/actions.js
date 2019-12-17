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

class ProductTeaser {
    constructor(rootElement) {
        rootElement.forEach(node => {
            const actionButton = node.querySelector(`.productteaser__cta button`);
            const action = actionButton.dataset['action'];
            let actionHandler;
            if (action === 'addToCart') {
                actionHandler = this._addToCartHandler;
            } else if (action === 'details') {
                actionHandler = this._seeDetailsHandler;
            } else {
                actionHandler = () => {
                    /* NOOP */
                };
            }

            actionButton.addEventListener('click', ev => {
                const element = ev.currentTarget;
                actionHandler(element);
            });
        });
    }

    _addToCartHandler(button) {
        const sku = button.dataset['itemSku'];
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: { sku, quantity: 1 }
        });
        document.dispatchEvent(customEvent);
    }

    _seeDetailsHandler(button) {
        const url = button.dataset['url'];
        window.location = url;
    }
}

ProductTeaser.selectors = {
    rootElement: '[data-cmp-is=productteaser]'
};

(function(doc) {
    function onDocumentReady() {
        const rootElement = doc.querySelectorAll(ProductTeaser.selectors.rootElement);
        new ProductTeaser(rootElement);
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);
