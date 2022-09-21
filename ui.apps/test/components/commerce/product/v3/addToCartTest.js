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

import AddToCart from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v3/product/clientlib/js/addToCart.js';

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
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-cmp-is="product" data-uid-cart data-product-sku="my-sample-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">my-sample-sku</strong>
                    </section>
                    <section class="productFullDetail__actions productFullDetail__section">
                        <button class="button__root_highPriority" data-cmp-is="add-to-cart">Add to cart</button>
                    </section>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <select name="quantity" data-uid-cart data-product-sku="my-sample-sku">
                            <option value="5" selected></option>
                        </select>
                    </section>
                </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);
        });

        it('initializes an AddToCart component for a configurable product', () => {
            productRoot.dataset.configurable = true;

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            assert.isTrue(addToCart._state.configurable);
            assert.isNull(addToCart._state.sku);
            assert.isTrue(addToCartRoot.disabled);
        });

        it('initializes an AddToCart component for a simple product', () => {
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            assert.isFalse(addToCart._state.configurable);
            assert.equal(addToCart._state.sku, 'my-sample-sku');
            assert.isFalse(addToCartRoot.disabled);
        });

        it('is disabled on invalid variant', () => {
            productRoot.dataset.configurable = true;
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            // Send event
            let changeEvent = new CustomEvent(AddToCart.events.variantChanged, {
                bubbles: true,
                detail: {}
            });
            productRoot.dispatchEvent(changeEvent);

            assert.isTrue(addToCartRoot.disabled);
        });

        it('reacts to a variantchanged event', () => {
            let spy = sinon.spy();
            document.addEventListener('aem.cif.add-to-cart', spy);
            productRoot.dataset.configurable = true;

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });

            // Send event
            let changeEvent = new CustomEvent(AddToCart.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: {
                        sku: 'variant-sku',
                        name: 'Variant ABC',
                        priceRange: {
                            finalPrice: 10,
                            currency: 'USD',
                            regularPrice: 10
                        }
                    },
                    attributes: {
                        color: 'red'
                    },
                    selections: {
                        color: 'red'
                    }
                }
            });
            productRoot.dispatchEvent(changeEvent);

            assert.equal(addToCart._state.sku, 'variant-sku');
            assert.deepEqual(addToCart._state.attributes, { color: 'red' });
            assert.isFalse(addToCartRoot.disabled);

            // verify that the storefrontData of the variant is passed to the add to cart event on click
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].detail[0].sku, 'variant-sku');
            assert.equal(spy.getCall(0).args[0].detail[0].parentSku, 'my-sample-sku');
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 5);
            assert.isFalse(spy.getCall(0).args[0].detail[0].virtual);
            assert.deepEqual(spy.getCall(0).args[0].detail[0].storefrontData, {
                name: 'Variant ABC',
                sku: 'variant-sku',
                regularPrice: 10,
                finalPrice: 10,
                currencyCode: 'USD',
                selectedOptions: [
                    {
                        attribute: 'color',
                        value: 'red'
                    }
                ]
            });
        });

        it('dispatches an event on click', () => {
            let spy = sinon.spy();
            document.addEventListener('aem.cif.add-to-cart', spy);

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].detail[0].sku, addToCart._state.sku);
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 5);
            assert.isFalse(spy.getCall(0).args[0].detail[0].virtual);
            assert.isUndefined(spy.getCall(0).args[0].detail[0].storefrontData);
        });

        it('dispatches an event on click with storefrontData', () => {
            let spy = sinon.spy();
            document.addEventListener('aem.cif.add-to-cart', spy);

            productRoot.dataset.cifProductContext = JSON.stringify({
                name: 'My Sample Product',
                pricing: {
                    regularPrice: 159.9,
                    specialPrice: 110.0,
                    currencyCode: 'USD'
                }
            });

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].detail[0].sku, addToCart._state.sku);
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 5);
            assert.deepEqual(spy.getCall(0).args[0].detail[0].storefrontData, {
                name: 'My Sample Product',
                regularPrice: 159.9,
                finalPrice: 110.0,
                currencyCode: 'USD'
            });
        });

        it('dispatches a virtual add to cart event', () => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML(
                'afterbegin',
                `<div data-cmp-is="product" data-virtual data-uid-cart data-product-sku="my-sample-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">my-sample-sku</strong>
                    </section>
                    <section class="productFullDetail__actions productFullDetail__section">
                        <button class="button__root_highPriority" data-cmp-is="add-to-cart">Add to cart</button>
                    </section>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <select name="quantity" data-uid-cart data-product-sku="my-sample-sku">
                            <option value="4" selected></option>
                        </select>
                    </section>
                </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);

            let spy = sinon.spy();
            document.addEventListener('aem.cif.add-to-cart', spy);

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            addToCartRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].type, 'aem.cif.add-to-cart');
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 4);
            assert.isTrue(spy.getCall(0).args[0].detail[0].virtual);
        });
    });
});
