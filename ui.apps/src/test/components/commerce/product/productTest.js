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
        let windowCIF;

        const clientPrices = { 'sample-sku': { currency: 'USD', value: '156.89' } };

        before(() => {
            // Create empty context
            windowCIF = window.CIF;
            window.CIF = {};

            window.CIF.PriceFormatter = class {
                formatPrice(price) {}
            };
            sinon.stub(window.CIF.PriceFormatter.prototype, 'formatPrice').returns('$123.45');

            window.CIF.CommerceGraphqlApi = {
                getProductPrices: sinon.stub().resolves(clientPrices)
            };
        });

        after(() => {
            // Restore original context
            window.CIF = windowCIF;
        });

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

        it('retrieves prices via GraphQL', () => {
            productRoot.dataset.loadClientPrice = true;
            let product = productCtx.factory({ element: productRoot });
            assert.isTrue(product._state.loadPrices);

            return product._initPrices().then(() => {
                assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
                assert.deepEqual(product._state.prices, clientPrices);

                let price = productRoot.querySelector(productCtx.Product.selectors.price).innerText;
                assert.include(price, '123.45');
            });
        });

        it('skips retrieving of prices via GraphQL when data attribute is not set', () => {
            let product = productCtx.factory({ element: productRoot });
            assert.isFalse(product._state.loadPrices);
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

        it('changes variant with client-side price when receiving variantchanged event', () => {
            let product = productCtx.factory({ element: productRoot });
            product._state.prices = {
                'variant-sku': {
                    currency: 'USD',
                    value: 130.42
                }
            };

            // Send event
            let variant = { sku: 'variant-sku' };
            let changeEvent = new CustomEvent(productCtx.Product.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: variant
                }
            });
            productRoot.dispatchEvent(changeEvent);

            // Check fields
            let price = productRoot.querySelector(productCtx.Product.selectors.price).innerText;
            assert.include(price, '123.45');
        });
    });
});
