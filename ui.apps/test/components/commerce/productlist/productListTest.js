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

import ProductList from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productlist/v1/productlist/clientlibs/js/productlist.js';

describe('Productlist', () => {
    let listRoot;
    let windowCIF;

    const clientPrices = {
        'sku-a': { currency: 'USD', value: '156.89' },
        'sku-b': { currency: 'USD', value: '123.45' },
        'sku-c': { currency: 'USD', value: '0.0' }
    };

    before(() => {
        // Create empty context
        windowCIF = window.CIF;
        window.CIF = {};

        window.CIF.PriceFormatter = class {
            formatPrice(price) {}
        };
        sinon.stub(window.CIF.PriceFormatter.prototype, 'formatPrice').callsFake(p => p.value);
    });

    after(() => {
        // Restore original context
        window.CIF = windowCIF;
    });

    beforeEach(() => {
        listRoot = document.createElement('div');
        listRoot.insertAdjacentHTML(
            'afterbegin',
            `<div class="item__root" data-sku="sku-a" role="product">
                <div class="item__price">
                    <span role="price">123</span>
                </div>
            </div>
            <div class="item__root" data-sku="sku-b" role="product">
                <div class="item__price">
                    <span role="price">456</span>
                </div>
            </div>
            <div class="item__root" data-sku="sku-c" role="product">
                <div class="item__price">
                    <span role="price">789</span>
                </div>
            </div>`
        );

        window.CIF.CommerceGraphqlApi = {
            getProductPrices: sinon.stub().resolves(clientPrices)
        };
    });

    it('initializes a product list component', () => {
        let list = new ProductList({ element: listRoot });

        assert.deepEqual(list._state.skus, ['sku-a', 'sku-b', 'sku-c']);
    });

    it('retrieves prices via GraphQL', () => {
        listRoot.dataset.loadClientPrice = true;
        let list = new ProductList({ element: listRoot });
        assert.isTrue(list._state.loadPrices);

        return list._fetchPrices().then(() => {
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
            assert.deepEqual(list._state.prices, clientPrices);

            // Verify price updates
            assert.include(listRoot.querySelector('[data-sku=sku-a] [role=price]').innerText, '156.89');
            assert.include(listRoot.querySelector('[data-sku=sku-b] [role=price]').innerText, '123.45');
            assert.include(listRoot.querySelector('[data-sku=sku-c] [role=price]').innerText, '0.0');
        });
    });

    it('skips retrieving of prices if CommerceGraphqlApi is not available', () => {
        delete window.CIF.CommerceGraphqlApi;

        listRoot.dataset.loadClientPrice = true;
        let list = new ProductList({ element: listRoot });
        assert.isTrue(list._state.loadPrices);

        list._fetchPrices();
        assert.isEmpty(list._state.prices);
    });

    it('skips retrieving of prices via GraphQL when data attribute is not set', () => {
        let list = new ProductList({ element: listRoot });
        assert.isFalse(list._state.loadPrices);
    });
});
