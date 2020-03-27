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
        'sku-a': {
            __typename: 'SimpleProduct',
            minimum_price: {
                regular_price: {
                    value: 156.89,
                    currency: 'USD'
                },
                final_price: {
                    value: 156.89,
                    currency: 'USD'
                },
                discount: {
                    amount_off: 0,
                    percent_off: 0
                }
            }
        },
        'sku-b': {
            __typename: 'ConfigurableProduct',
            minimum_price: {
                regular_price: {
                    value: 123.45,
                    currency: 'USD'
                },
                final_price: {
                    value: 123.45,
                    currency: 'USD'
                },
                discount: {
                    amount_off: 0,
                    percent_off: 0
                }
            },
            maximum_price: {
                regular_price: {
                    value: 150.45,
                    currency: 'USD'
                },
                final_price: {
                    value: 150.45,
                    currency: 'USD'
                },
                discount: {
                    amount_off: 0,
                    percent_off: 0
                }
            }
        },
        'sku-c': {
            __typename: 'SimpleProduct',
            minimum_price: {
                regular_price: {
                    value: 20,
                    currency: 'USD'
                },
                final_price: {
                    value: 10,
                    currency: 'USD'
                },
                discount: {
                    amount_off: 10,
                    percent_off: 50
                }
            }
        },
        'sku-d': {
            __typename: 'GroupedProduct',
            minimum_price: {
                regular_price: {
                    value: 20,
                    currency: 'USD'
                },
                final_price: {
                    value: 20,
                    currency: 'USD'
                },
                discount: {
                    amount_off: 0,
                    percent_off: 0
                }
            }
        }
    };

    const convertedPrices = {
        'sku-a': {
            isStartPrice: false,
            currency: 'USD',
            regularPrice: 156.89,
            finalPrice: 156.89,
            discountAmount: 0,
            discountPercent: 0,
            discounted: false,
            range: false
        },
        'sku-b': {
            isStartPrice: false,
            currency: 'USD',
            regularPrice: 123.45,
            finalPrice: 123.45,
            discountAmount: 0,
            discountPercent: 0,
            regularPriceMax: 150.45,
            finalPriceMax: 150.45,
            discountAmountMax: 0,
            discountPercentMax: 0,
            discounted: false,
            range: true
        },
        'sku-c': {
            isStartPrice: false,
            currency: 'USD',
            regularPrice: 20,
            finalPrice: 10,
            discountAmount: 10,
            discountPercent: 50,
            discounted: true,
            range: false
        },
        'sku-d': {
            isStartPrice: true,
            currency: 'USD',
            regularPrice: 20,
            finalPrice: 20,
            discountAmount: 0,
            discountPercent: 0,
            discounted: false,
            range: false
        }
    };

    before(() => {
        // Create empty context
        windowCIF = window.CIF;
        window.CIF = {};

        window.CIF.PriceFormatter = class {
            formatPrice(price) {}
        };
        sinon
            .stub(window.CIF.PriceFormatter.prototype, 'formatPrice')
            .callsFake(price => price.currency + ' ' + price.value);
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
                <div class="price">
                    <span>123</span>
                </div>
            </div>
            <div class="item__root" data-sku="sku-b" role="product">
                <div class="price">
                    <span>456</span>
                </div>
            </div>
            <div class="item__root" data-sku="sku-c" role="product">
                <div class="price">
                    <span>789</span>
                </div>
            </div>
            <div class="item__root" data-sku="sku-d" role="product">
                <div class="price">
                    <span>101112</span>
                </div>
            </div>`
        );

        window.CIF.CommerceGraphqlApi = {
            getProductPrices: sinon.stub().resolves(clientPrices)
        };
    });

    it('initializes a product list component', () => {
        let list = new ProductList({ element: listRoot });

        assert.deepEqual(list._state.skus, ['sku-a', 'sku-b', 'sku-c', 'sku-d']);
    });

    it('retrieves prices via GraphQL', () => {
        listRoot.dataset.loadClientPrice = true;
        let list = new ProductList({ element: listRoot });
        assert.isTrue(list._state.loadPrices);

        return list._fetchPrices().then(() => {
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
            assert.deepEqual(list._state.prices, convertedPrices);

            // Verify price updates
            assert.include(listRoot.querySelector('[data-sku=sku-a] .price').innerText, 'USD 156.89');
            assert.include(listRoot.querySelector('[data-sku=sku-b] .price').innerText, 'USD 123.45 - USD 150.45');
            assert.include(listRoot.querySelector('[data-sku=sku-c] .price').innerText, 'USD 20');
            assert.include(listRoot.querySelector('[data-sku=sku-c] .price').innerText, 'USD 10');
            assert.include(listRoot.querySelector('[data-sku=sku-d] .price').innerText, 'Starting at USD 20');
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
