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

    let productRoot;

    beforeEach(() => {
        let sku = document.createElement('span');
        sku.setAttribute('role', 'sku');

        let details = document.createElement('div');
        details.classList.add('productFullDetail__details');
        details.appendChild(sku);
        
        productRoot = document.createElement('div');
        productRoot.appendChild(details);
    });

    it('initializes a configurable product component', () => {
        productRoot.dataset.configurable = true;

        let product = productCtx.factory({ element: productRoot });
        assert.isTrue(product._state.configurable);
        assert.isNull(product._state.sku);
    });

    it('initializes a simple product component', () => {
        productRoot.querySelector(productCtx.Product.selectors.sku).innerHTML = 'sample-sku';

        let product = productCtx.factory({ element: productRoot });
        assert.isFalse(product._state.configurable);
        assert.equal(product._state.sku, 'sample-sku');
    });

    it.skip('changes variant when receiving variantchanged event', () => {
        /* let root = document.createElement('div');
        let product = new Product({ element: root });

        console.log("Hello world, I'm a test.");
        console.log(product); */
    });


});