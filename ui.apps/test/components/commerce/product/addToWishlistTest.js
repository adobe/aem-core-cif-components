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

import AddToWishlist from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product/clientlib/js/addToWishlist.js';

describe('Product', () => {
    describe('AddToWishlist', () => {
        let productRoot;
        let addToWishlistRoot;
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
                `<div data-cmp-is="product">
                    <div class="productFullDetail__details">
                        <span role="sku">my-sample-sku</span>
                    </div>
                    <div class="productFullDetail__cartActions">
                        <button class="button__root_normalPriority">
                    </div>
                    <div class="productFullDetail__quantity">
                        <select data-product-sku="my-sample-sku">
                            <option value="5" selected></option>
                        </select>
                    </div>
                </div>`
            );

            addToWishlistRoot = pageRoot.querySelector(AddToWishlist.selectors.self);
            productRoot = pageRoot.querySelector(AddToWishlist.selectors.product);
        });

        it('dispatches an event on click', () => {
            let spy = sinon.spy();
            let _originalDispatch = document.dispatchEvent;
            document.dispatchEvent = spy;
            let addToWishlist = new AddToWishlist({ element: addToWishlistRoot, product: productRoot });
            addToWishlistRoot.click();
            sinon.assert.calledOnce(spy);
            assert.equal(spy.getCall(0).args[0].type, 'aem.cif.add-to-wishlist');
            assert.equal(spy.getCall(0).args[0].detail[0].sku, 'my-sample-sku');
            assert.equal(spy.getCall(0).args[0].detail[0].quantity, 5);
            document.dispatchEvent = _originalDispatch;
        });
    });
});
