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

import AddToCart from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product/clientlib/js/addToCart.js';

describe('GroupedProduct', () => {
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
                `<div data-cmp-is="product" data-grouped>
                    <div class="productFullDetail__details">
                        <span role="sku">my-grouped-product</span>
                    </div>
                    <div class="productFullDetail__cartActions">
                        <button class="button__root_highPriority">
                    </div>
                    <section class="productFullDetail__groupedProducts productFullDetail__quantity">
                        <table>
                            <tr data-cif-grouped-product-context="{&#34;pricing&#34;:{&#34;regularPrice&#34;:11.0,&#34;specialPrice&#34;:11.0,&#34;currencyCode&#34;:&#34;USD&#34;},&#34;sku&#34;:&#34;sku1&#34;,&#34;name&#34;:&#34;SKU 1&#34;}">
                                <td>
                                    <select data-product-sku="sku1">
                                        <option value="0" selected></option>
                                        <option value="1"></option>
                                    </select>
                                </td>
                            </tr>
                            <tr data-cif-grouped-product-context="{&#34;pricing&#34;:{&#34;regularPrice&#34;:22.0,&#34;specialPrice&#34;:22.0,&#34;currencyCode&#34;:&#34;USD&#34;},&#34;sku&#34;:&#34;sku2&#34;,&#34;name&#34;:&#34;SKU 2&#34;}">
                                <td>
                                    <select data-product-sku="sku2">
                                        <option value="0" selected></option>
                                        <option value="1"></option>
                                    </select>
                                </td>
                            </tr>
                            <tr data-cif-grouped-product-context="{&#34;pricing&#34;:{&#34;regularPrice&#34;:33.0,&#34;specialPrice&#34;:33.0,&#34;currencyCode&#34;:&#34;USD&#34;},&#34;sku&#34;:&#34;sku3&#34;,&#34;name&#34;:&#34;SKU 3&#34;}">
                                <td>
                                    <select data-product-sku="sku3">
                                        <option value="0" selected></option>
                                        <option value="1"></option>
                                    </select>
                                </td>
                            </tr>
                            <tr data-cif-grouped-product-context="{&#34;pricing&#34;:{&#34;regularPrice&#34;:44.0,&#34;specialPrice&#34;:44.0,&#34;currencyCode&#34;:&#34;USD&#34;},&#34;sku&#34;:&#34;sku4&#34;,&#34;name&#34;:&#34;SKU 4&#34;}">
                                <td>
                                    <select data-product-sku="sku4" data-virtual>
                                        <option value="0" selected></option>
                                        <option value="1"></option>
                                    </select>
                                </td>
                            </tr>
                        </table>
                    </section>
                </div>`
            );

            addToCartRoot = pageRoot.querySelector(AddToCart.selectors.self);
            productRoot = pageRoot.querySelector(AddToCart.selectors.product);
        });

        it('initializes an AddToCart component for a grouped product', () => {
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            assert.isTrue(addToCartRoot.disabled);
        });

        it('enables/disables AddToCart button based on quantity selection', () => {
            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            let selections = Array.from(pageRoot.querySelectorAll(AddToCart.selectors.quantity));

            // Select quantity "1" for first product
            selections[0].selectedIndex = 1;
            selections[0].dispatchEvent(new Event('change'));
            assert.isFalse(addToCartRoot.disabled);

            // Select quantity "0" for first product
            selections[0].selectedIndex = 0;
            selections[0].dispatchEvent(new Event('change'));
            assert.isTrue(addToCartRoot.disabled);
        });

        it('dispatches add-to-cart event on click', () => {
            let spy = sinon.spy();
            document.addEventListener('aem.cif.add-to-cart', spy);

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            let selections = Array.from(pageRoot.querySelectorAll(AddToCart.selectors.quantity));

            // Select quantity "1" for two products
            selections[0].selectedIndex = 1;
            selections[2].selectedIndex = 1;
            selections[0].dispatchEvent(new Event('change'));
            assert.isFalse(addToCartRoot.disabled);
            addToCartRoot.click();

            sinon.assert.calledOnce(spy);
            let event = spy.getCall(0).args[0];
            assert.equal(event.type, AddToCart.events.addToCart);
            assert.equal(event.detail.length, 2);
            assert.equal(event.detail[0].sku, 'sku1');
            assert.equal(event.detail[0].quantity, 1);
            assert.deepEqual(event.detail[0].storefrontData, {
                name: 'SKU 1',
                regularPrice: 11.0,
                finalPrice: 11.0,
                currencyCode: 'USD'
            });
            assert.equal(event.detail[1].sku, 'sku3');
            assert.equal(event.detail[1].quantity, 1);
            assert.deepEqual(event.detail[1].storefrontData, {
                name: 'SKU 3',
                regularPrice: 33.0,
                finalPrice: 33.0,
                currencyCode: 'USD'
            });
        });

        it('dispatches add-to-cart event with virtual product on click', () => {
            let spy = sinon.spy();
            document.addEventListener('aem.cif.add-to-cart', spy);

            let addToCart = new AddToCart({ element: addToCartRoot, product: productRoot });
            let selections = Array.from(pageRoot.querySelectorAll(AddToCart.selectors.quantity));

            // Select quantity "1" for two products
            selections[0].selectedIndex = 1;
            selections[3].selectedIndex = 1;
            selections[0].dispatchEvent(new Event('change'));
            assert.isFalse(addToCartRoot.disabled);
            addToCartRoot.click();

            sinon.assert.calledOnce(spy);
            let event = spy.getCall(0).args[0];
            assert.equal(event.type, AddToCart.events.addToCart);
            assert.equal(event.detail.length, 2);
            assert.equal(event.detail[0].sku, 'sku1');
            assert.equal(event.detail[0].quantity, 1);
            assert.isFalse(event.detail[0].virtual);
            assert.equal(event.detail[1].sku, 'sku4');
            assert.equal(event.detail[1].quantity, 1);
            assert.isTrue(event.detail[1].virtual);
        });
    });
});
