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

    describe('AddToCart', () => {

        let productRoot;
        let addToCartRoot;
        let pageRoot;

        before(() => {
            let body = document.querySelector('body');
            pageRoot = document.createElement('div');
            body.appendChild(pageRoot);
        });

        after(() => {
            pageRoot.parentNode.removeChild(pageRoot);
        });

        beforeEach(() => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML('afterbegin', `
                <div data-cmp-is="product">
                    <div class="productFullDetail__details">
                        <span role="sku">my-sample-sku</span>
                    </div>
                    <div class="productFullDetail__cartActions">
                        <button>
                    </div>
                    <div class="productFullDetail__quantity">
                        <select>
                            <option value="5" selected></option>
                        </select>
                    </div>
                </div>
            `);

            addToCartRoot = pageRoot.querySelector(addToCartCtx.AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(addToCartCtx.AddToCart.selectors.product);
        });

        it('initializes an AddToCart component for a configurable product', () => {
            productRoot.dataset.configurable = true;

            let addToCart = addToCartCtx.factory({ element: addToCartRoot, product: productRoot });

            assert.isTrue(addToCart._state.configurable);
            assert.isNull(addToCart._state.sku);
            assert.isTrue(addToCartRoot.disabled);
        });

        it('initializes an AddToCart component for a simple product', () => {
            let addToCart = addToCartCtx.factory({ element: addToCartRoot, product: productRoot });

            assert.isFalse(addToCart._state.configurable);
            assert.equal(addToCart._state.sku, "my-sample-sku");
            assert.isFalse(addToCartRoot.disabled);
        });

        it('is disabled on invalid variant', () => {
            productRoot.dataset.configurable = true;
            let addToCart = addToCartCtx.factory({ element: addToCartRoot, product: productRoot });

            // Send event
            let changeEvent = new CustomEvent(addToCartCtx.AddToCart.events.variantChanged, {
                bubbles: true,
                detail: {}
            });
            productRoot.dispatchEvent(changeEvent);

            assert.isTrue(addToCartRoot.disabled);
        });

        it('reacts to a variantchanged event', () => {
            productRoot.dataset.configurable = true;
            let addToCart = addToCartCtx.factory({ element: addToCartRoot, product: productRoot });

            // Send event
            let changeEvent = new CustomEvent(addToCartCtx.AddToCart.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: {
                        sku: 'variant-sku'
                    },
                    attributes: {
                        color: 'red'
                    }
                }
            });
            productRoot.dispatchEvent(changeEvent);

            assert.equal(addToCart._state.sku, 'variant-sku');
            assert.deepEqual(addToCart._state.attributes, { color: 'red' });
            assert.isFalse(addToCartRoot.disabled);
        });

        it('adds a product to the cart on click', () => {
            let spy = sinon.spy(addToCartCtx.AddToCart.prototype, '_onAddToCart');
            window.CIF = {
                MiniCart: {
                    addItem: sinon.spy()
                }
            }
            let addToCart = addToCartCtx.factory({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();

            spy.restore();

            assert.isTrue(spy.called);
        });

    });
});