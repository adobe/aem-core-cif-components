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

import Product from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product/clientlib/js/product.js';
import CommerceGraphqlApi from '../../../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js';

describe('GroupedProduct', () => {
    describe('Core', () => {
        let productRoot;
        let windowCIF;

        const clientPrices = {
            'grouped-product-sku': {
                __typename: 'GroupedProduct',
                minimum_price: {
                    regular_price: {
                        value: 14,
                        currency: 'USD'
                    },
                    final_price: {
                        value: 14,
                        currency: 'USD'
                    },
                    discount: {
                        amount_off: 0,
                        percent_off: 0
                    }
                }
            },
            sku1: {
                __typename: 'GroupedProduct',
                minimum_price: {
                    regular_price: {
                        value: 14,
                        currency: 'USD'
                    },
                    final_price: {
                        value: 14,
                        currency: 'USD'
                    },
                    discount: {
                        amount_off: 0,
                        percent_off: 0
                    }
                }
            },
            sku2: {
                __typename: 'GroupedProduct',
                minimum_price: {
                    regular_price: {
                        value: 17,
                        currency: 'USD'
                    },
                    final_price: {
                        value: 17,
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
            'grouped-product-sku': {
                productType: 'GroupedProduct',
                currency: 'USD',
                regularPrice: 14,
                finalPrice: 14,
                discountAmount: 0,
                discountPercent: 0,
                discounted: false,
                range: false
            },
            sku1: {
                productType: 'GroupedProduct',
                currency: 'USD',
                regularPrice: 14,
                finalPrice: 14,
                discountAmount: 0,
                discountPercent: 0,
                discounted: false,
                range: false
            },
            sku2: {
                productType: 'GroupedProduct',
                currency: 'USD',
                regularPrice: 17,
                finalPrice: 17,
                discountAmount: 0,
                discountPercent: 0,
                discounted: false,
                range: false
            }
        };

        before(() => {
            // Create empty context
            windowCIF = window.CIF;
            window.CIF = { ...window.CIF };

            // We mock the Granite i18n support to also test that part of the PriceFormatter
            window.Granite = {};
            window.Granite.I18n = {
                setLocale: () => {}, // noop
                get: key => key
            };

            window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({ graphqlEndpoint: 'https://foo.bar/graphql' });
            window.CIF.CommerceGraphqlApi.getProductPrices = sinon.stub().resolves(clientPrices);
        });

        after(() => {
            // Restore original context
            window.CIF = windowCIF;
        });

        beforeEach(() => {
            productRoot = document.createElement('div');
            productRoot.dataset.locale = 'en-US'; // enforce the locale for prices
            productRoot.insertAdjacentHTML(
                'afterbegin',
                `<div class="productFullDetail__details">
                    <span role="sku">grouped-product-sku</span>
                </div>
                <section class="productFullDetail__groupedProducts">
                    <div class="price" data-product-sku="sku1"></div>
                    <div class="price" data-product-sku="sku2"></div>
                </section>`
            );
        });

        it('retrieves prices via GraphQL', () => {
            productRoot.dataset.loadClientPrice = true;
            let product = new Product({ element: productRoot });
            assert.isTrue(product._state.loadPrices);

            return product._initPrices().then(() => {
                assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
                assert.deepEqual(product._state.prices, convertedPrices);

                let price = productRoot.querySelector(Product.selectors.price + '[data-product-sku="sku1"]').innerText;
                assert.equal(price, '$14.00');

                price = productRoot.querySelector(Product.selectors.price + '[data-product-sku="sku2"]').innerText;
                assert.equal(price, '$17.00');
            });
        });
    });
});
