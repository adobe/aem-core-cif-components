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

describe('Product', () => {
    describe('Core', () => {
        let productRoot;

        beforeEach(() => {
            productRoot = document.createElement('div');
            productRoot.insertAdjacentHTML(
                'afterbegin',
                `<div class="productFullDetail__title">
                    <span role="name"></span>
                </div>
                <div class="productFullDetail__details">
                    <span role="sku">sample-sku</span>
                </div>
                <div class="productFullDetail__productPrice">
                    <span role="price"></span>
                </div>
                <div class="productFullDetail__description">
                    <span role="description"></span>
                </div>`
            );
        });

        it('initializes a configurable product component', () => {
            productRoot.dataset.configurable = true;

            let product = productCtx.factory({ element: productRoot });
            assert.isTrue(product._state.configurable);
            assert.equal(product._state.sku, 'sample-sku');
        });

        it('initializes a simple product component', () => {
            let product = productCtx.factory({ element: productRoot });
            assert.isFalse(product._state.configurable);
            assert.equal(product._state.sku, 'sample-sku');
        });

        it('changes variant when receiving variantchanged event', () => {
            let product = productCtx.factory({ element: productRoot });

            // Send event
            let variant = {
                sku: 'variant-sku',
                name: 'Variant Name',
                formattedPrice: '129,41 USD',
                description: '<p>abc</p>'
            };
            let changeEvent = new CustomEvent(productCtx.Product.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: variant
                }
            });
            productRoot.dispatchEvent(changeEvent);

            // Check state
            assert.equal(product._state.sku, variant.sku);

            // Check fields
            let sku = productRoot.querySelector(productCtx.Product.selectors.sku).innerText;
            let name = productRoot.querySelector(productCtx.Product.selectors.name).innerText;
            let price = productRoot.querySelector(productCtx.Product.selectors.price).innerText;
            let description = productRoot.querySelector(productCtx.Product.selectors.description).innerHTML;

            assert.equal(sku, variant.sku);
            assert.equal(name, variant.name);
            assert.equal(price, variant.formattedPrice);
            assert.equal(description, variant.description);
        });
    });
});
