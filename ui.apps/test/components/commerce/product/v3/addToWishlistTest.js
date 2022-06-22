/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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

import AddToWishlist from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v3/product/clientlib/js/addToWishlist.js';

describe('Product', () => {
    describe('AddToWishlist', () => {
        const dispatchEventSpy = sinon.spy();
        const originalDispatchEvent = document.dispatchEvent;

        let productRoot;
        let addToWishlistRoot;
        let pageRoot;

        const setupPage = html => {
            while (pageRoot.firstChild) {
                pageRoot.removeChild(pageRoot.firstChild);
            }
            pageRoot.insertAdjacentHTML('afterbegin', html);

            addToWishlistRoot = pageRoot.querySelector(AddToWishlist.selectors.self);
            productRoot = pageRoot.querySelector(AddToWishlist.selectors.product);
        };

        before(() => {
            let body = document.querySelector('body');
            pageRoot = document.createElement('div');
            body.appendChild(pageRoot);
            document.dispatchEvent = dispatchEventSpy;
        });

        after(() => {
            pageRoot.parentNode.removeChild(pageRoot);
            document.dispatchEvent = originalDispatchEvent;
        });

        afterEach(() => {
            dispatchEventSpy.resetHistory();
        });

        it('dispatches an event on click', () => {
            setupPage(`
                <div data-cmp-is="product" data-uid-cart data-product-sku="my-sample-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">my-sample-sku</strong>
                    </section>
                    <section class="productFullDetail__actions productFullDetail__section">
                        <button class="button__root_normalPriority" data-cmp-is="add-to-wish-list">Add to Wishlist</button>
                    </section>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <select name="quantity" data-uid-cart data-product-sku="my-sample-sku">
                            <option value="5" selected></option>
                        </select>
                    </section>
                </div>
            `);

            let addToWishlist = new AddToWishlist({ element: addToWishlistRoot, product: productRoot });
            addToWishlistRoot.click();

            sinon.assert.calledOnce(dispatchEventSpy);
            assert.equal(dispatchEventSpy.getCall(0).args[0].type, 'aem.cif.add-to-wishlist');
            assert.equal(dispatchEventSpy.getCall(0).args[0].detail[0].sku, 'my-sample-sku');
            assert.equal(dispatchEventSpy.getCall(0).args[0].detail[0].quantity, 5);
        });

        it('dispatches an event on click for grouped product', () => {
            setupPage(`
                <div data-cmp-is="product" data-uid-cart data-grouped data-product-sku="parent-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">parent-sku</strong>
                    </section>
                    <section class="productFullDetail__actions productFullDetail__section">
                        <button class="button__root_normalPriority" data-cmp-is="add-to-wish-list">Add to Wishlist</button>
                    </section>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <select data-use-uid data-product-sku="my-sample-sku">
                            <option value="5" selected></option>
                        </select>
                    </section>
                </div>
            `);

            let addToWishlist = new AddToWishlist({ element: addToWishlistRoot, product: productRoot });
            addToWishlistRoot.click();

            sinon.assert.calledOnce(dispatchEventSpy);
            assert.equal(dispatchEventSpy.getCall(0).args[0].type, 'aem.cif.add-to-wishlist');
            assert.equal(dispatchEventSpy.getCall(0).args[0].detail[0].sku, 'parent-sku');
            assert.equal(dispatchEventSpy.getCall(0).args[0].detail[0].quantity, 1);
        });

        it('dispatches an event on click with variant selected', () => {
            setupPage(`
                <div data-cmp-is="product" data-uid-cart data-product-sku="my-sample-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">my-sample-sku</strong>
                    </section>
                    <section class="productFullDetail__actions productFullDetail__section">
                        <button class="button__root_normalPriority" data-cmp-is="add-to-wish-list">Add to Wishlist</button>
                    </section>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <select data-use-uid data-product-sku="my-sample-variant-sku">
                            <option value="4" selected></option>
                        </select>
                    </section>
                </div>
            `);

            let addToWishlist = new AddToWishlist({ element: addToWishlistRoot, product: productRoot });
            addToWishlistRoot.click();
            sinon.assert.calledOnce(dispatchEventSpy);

            assert.equal(dispatchEventSpy.getCall(0).args[0].type, 'aem.cif.add-to-wishlist');
            assert.equal(dispatchEventSpy.getCall(0).args[0].detail[0].sku, 'my-sample-sku');
            assert.equal(dispatchEventSpy.getCall(0).args[0].detail[0].quantity, 4);
        });

        it('dispatches no event if no product selected', () => {
            setupPage(`
                <div data-cmp-is="product" data-uid-cart data-product-sku="my-sample-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">my-sample-sku</strong>
                    </section>
                    <section class="productFullDetail__actions productFullDetail__section">
                        <button class="button__root_normalPriority" data-cmp-is="add-to-wish-list">Add to Wishlist</button>
                    </section>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <select name="quantity" data-uid-cart data-product-sku="my-sample-sku">
                            <option value="0" selected></option>
                        </select>
                    </section>
                </div>
            `);

            let addToWishlist = new AddToWishlist({ element: addToWishlistRoot, product: productRoot });
            addToWishlistRoot.click();

            sinon.assert.notCalled(dispatchEventSpy);
        });
    });
});
