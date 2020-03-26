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

import Product from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product/clientlib/js/product.js';

describe('Product', () => {
    describe('Core', () => {
        let productRoot;
        let windowCIF;

        const clientPrices = {
            'sample-sku': {
                __typename: 'SimpleProduct',
                minimum_price: {
                    regular_price: {
                        value: 98,
                        currency: 'USD'
                    },
                    final_price: {
                        value: 98,
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
            'sample-sku': {
                productType: 'SimpleProduct',
                currency: 'USD',
                regularPrice: 98,
                finalPrice: 98,
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
                <div class="price" data-product-sku="sample-sku"></div>
                <div class="productFullDetail__description">
                    <span role="description"></span>
                </div>`
            );
        });

        it('initializes a configurable product component', () => {
            productRoot.dataset.configurable = true;

            let product = new Product({ element: productRoot });
            assert.isTrue(product._state.configurable);
            assert.equal(product._state.sku, 'sample-sku');
        });

        it('initializes a simple product component', () => {
            let product = new Product({ element: productRoot });
            assert.isFalse(product._state.configurable);
            assert.equal(product._state.sku, 'sample-sku');
        });

        it('retrieves prices via GraphQL', () => {
            productRoot.dataset.loadClientPrice = true;
            let product = new Product({ element: productRoot });
            assert.isTrue(product._state.loadPrices);

            return product._initPrices().then(() => {
                assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
                assert.deepEqual(product._state.prices, convertedPrices);

                let price = productRoot.querySelector(Product.selectors.price).innerText;
                assert.include(price, 'USD 98');
            });
        });

        it('skips retrieving of prices via GraphQL when data attribute is not set', () => {
            let product = new Product({ element: productRoot });
            assert.isFalse(product._state.loadPrices);
        });

        it('changes variant when receiving variantchanged event', () => {
            let product = new Product({ element: productRoot });

            // Send event
            let variant = {
                sku: 'variant-sku',
                name: 'Variant Name',
                priceRange: convertedPrices['sample-sku'],
                description: '<p>abc</p>'
            };
            let changeEvent = new CustomEvent(Product.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: variant
                }
            });
            productRoot.dispatchEvent(changeEvent);

            // Check state
            assert.equal(product._state.sku, variant.sku);

            // Check fields
            let sku = productRoot.querySelector(Product.selectors.sku).innerText;
            let name = productRoot.querySelector(Product.selectors.name).innerText;
            let price = productRoot.querySelector(Product.selectors.price).innerText;
            let description = productRoot.querySelector(Product.selectors.description).innerHTML;

            assert.equal(sku, variant.sku);
            assert.equal(name, variant.name);
            assert.equal(price, 'USD 98');
            assert.equal(description, variant.description);
        });

        it('changes variant with client-side price when receiving variantchanged event', () => {
            let product = new Product({ element: productRoot });
            product._state.prices = {
                'variant-sku': convertedPrices['sample-sku']
            };

            // Send event
            let variant = { sku: 'variant-sku' };
            let changeEvent = new CustomEvent(Product.events.variantChanged, {
                bubbles: true,
                detail: {
                    variant: variant
                }
            });
            productRoot.dispatchEvent(changeEvent);

            // Check fields
            let price = productRoot.querySelector(Product.selectors.price).innerText;
            assert.include(price, 'USD 98');
        });

        it('displays a price range', () => {
            const priceRange = {
                'sample-sku': {
                    minimum_price: {
                        regular_price: {
                            value: 10,
                            currency: 'USD'
                        },
                        final_price: {
                            value: 10,
                            currency: 'USD'
                        },
                        discount: {
                            amount_off: 0,
                            percent_off: 0
                        }
                    },
                    maximum_price: {
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
            window.CIF.CommerceGraphqlApi.getProductPrices.resetBehavior();
            window.CIF.CommerceGraphqlApi.getProductPrices.resolves(priceRange);

            productRoot.dataset.loadClientPrice = true;
            let product = new Product({ element: productRoot });

            return product._initPrices().then(() => {
                let price = productRoot.querySelector(Product.selectors.price).innerText;
                assert.include(price, 'USD 10 - USD 20');
            });
        });

        it('displays a discounted price', () => {
            const priceDiscount = {
                'sample-sku': {
                    minimum_price: {
                        regular_price: {
                            value: 80.12,
                            currency: 'USD'
                        },
                        final_price: {
                            value: 69.99,
                            currency: 'USD'
                        },
                        discount: {
                            amount_off: 10.13,
                            percent_off: 12.6
                        }
                    }
                }
            };
            window.CIF.CommerceGraphqlApi.getProductPrices.resetBehavior();
            window.CIF.CommerceGraphqlApi.getProductPrices.resolves(priceDiscount);

            productRoot.dataset.loadClientPrice = true;
            let product = new Product({ element: productRoot });

            return product._initPrices().then(() => {
                let regularPrice = productRoot.querySelector(Product.selectors.price + ' .regularPrice').innerText;
                let finalPrice = productRoot.querySelector(Product.selectors.price + ' .discountedPrice').innerText;
                let youSave = productRoot.querySelector(Product.selectors.price + ' .you-save').innerText;
                assert.include(regularPrice, 'USD 80.12');
                assert.include(finalPrice, 'USD 69.99');
                assert.include(youSave, 'USD 10.13');
                assert.include(youSave, '12.6%');
            });
        });

        it('displays a discounted price range', () => {
            const priceRange = {
                'sample-sku': {
                    minimum_price: {
                        regular_price: {
                            value: 10,
                            currency: 'USD'
                        },
                        final_price: {
                            value: 5,
                            currency: 'USD'
                        },
                        discount: {
                            amount_off: 5,
                            percent_off: 50
                        }
                    },
                    maximum_price: {
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
                }
            };
            window.CIF.CommerceGraphqlApi.getProductPrices.resetBehavior();
            window.CIF.CommerceGraphqlApi.getProductPrices.resolves(priceRange);

            productRoot.dataset.loadClientPrice = true;
            let product = new Product({ element: productRoot });

            return product._initPrices().then(() => {
                let regularPrice = productRoot.querySelector(Product.selectors.price + ' .regularPrice').innerText;
                let finalPrice = productRoot.querySelector(Product.selectors.price + ' .discountedPrice').innerText;
                let youSave = productRoot.querySelector(Product.selectors.price + ' .you-save').innerText;
                assert.include(regularPrice, 'USD 10 - USD 20');
                assert.include(finalPrice, 'USD 5 - USD 10');
                assert.include(youSave, 'USD 5');
                assert.include(youSave, '50%');
            });
        });
    });
});
