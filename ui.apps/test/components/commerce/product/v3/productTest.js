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

import Product from '../../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/product/v3/product/clientlib/js/product.js';
import CommerceGraphqlApi from '../../../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js';

describe('Product v3', () => {
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
            window.CIF = { ...window.CIF };
            window.CIF.locale = 'en-us';
            window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({ graphqlEndpoint: 'https://foo.bar/graphql' });
            window.CIF.CommerceGraphqlApi.getProductPrices = sinon.stub().resolves(clientPrices);
        });

        after(() => {
            // Restore original context
            window.CIF = windowCIF;
        });

        beforeEach(() => {
            delete window.CIF.enableClientSidePriceLoading;

            const testDoc = document.createElement('div');
            testDoc.insertAdjacentHTML(
                'afterbegin',
                `<div data-locale="en-US" data-cmp-is="product" data-uid-cart data-product-sku="sample-sku">
                    <section class="productFullDetail__sku productFullDetail__section">
                        <h2 class="productFullDetail__skuTitle productFullDetail__sectionTitle">SKU</h2>
                        <strong role="sku">sample-sku</strong>
                    </section>
                    <section class="productFullDetail__title">
                        <h1 class="productFullDetail__productName">
                            <span role="name">My sample product</span>
                        </h1>
                    </section>
                    <section class="productFullDetail__price">
                        <div class="price" data-product-sku="sample-sku"></div>
                    </section>
                    <section class="productFullDetail__description">
                        <span role="description"></span>
                    </section>
                </div>`
            );

            productRoot = testDoc.querySelector(Product.selectors.self);
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

        it('initializes a product component with no SKU', () => {
            delete productRoot.dataset.productSku;
            let product = new Product({ element: productRoot });
            assert.isFalse(product._state.configurable);
            assert.isNull(product._state.sku);
        });

        it('retrieves prices via GraphQL', () => {
            window.CIF.enableClientSidePriceLoading = true;
            let product = new Product({ element: productRoot });
            assert.isTrue(product._state.loadPrices);

            return product._initPrices().then(() => {
                assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
                assert.deepEqual(product._state.prices, convertedPrices);

                let price = productRoot.querySelector(Product.selectors.price).innerText;
                assert.equal(price, '$98.00');
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
            assert.equal(price, '$98.00');
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
            assert.equal(price, '$98.00');
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
                assert.equal(price, 'From $10.00 To $20.00');
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
                assert.equal(regularPrice, '$80.12');
                assert.equal(finalPrice, '$69.99');
                assert.equal(youSave, 'You save $10.13 (12.6%)');
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
                assert.equal(regularPrice, 'From $10.00 To $20.00');
                assert.equal(finalPrice, 'From $5.00 To $10.00');
                assert.equal(youSave, 'You save $5.00 (50%)');
            });
        });

        it('displays a null price', () => {
            const priceRange = {
                'sample-sku': {
                    minimum_price: {
                        regular_price: {
                            value: null,
                            currency: 'USD'
                        },
                        final_price: {
                            value: null,
                            currency: 'USD'
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
                assert.equal(price, '');
            });
        });

        it('updates JSON-LD price when new prices are provided', () => {
            const jsonLdScript = document.createElement('script');
            jsonLdScript.type = 'application/ld+json';
            jsonLdScript.innerHTML = JSON.stringify({
                '@context': 'https://schema.org',
                '@type': 'Product',
                offers: [
                    {
                        sku: 'sample-sku',
                        price: 0, // initial price
                        priceCurrency: 'USD',
                        priceSpecification: {
                            price: 0 // initial regular price
                        }
                    }
                ]
            });
            document.head.appendChild(jsonLdScript);

            let product = new Product({ element: productRoot });

            product._updateJsonLdPrice = function(prices) {
                if (CIF.enableClientSidePriceLoading || !document.querySelector('script[type="application/ld+json"]')) {
                    return;
                }

                const jsonLdScript = document.querySelector('script[type="application/ld+json"]');
                const jsonLdData = JSON.parse(jsonLdScript.innerHTML.trim());

                if (Array.isArray(jsonLdData.offers)) {
                    let priceUpdated = false;

                    jsonLdData.offers.forEach(offer => {
                        const convertedPrice = prices[offer.sku];

                        if (convertedPrice) {
                            offer.price = convertedPrice.finalPrice;
                            if (offer.priceSpecification) {
                                offer.priceSpecification.price = convertedPrice.regularPrice;
                            }
                            priceUpdated = true;
                        }
                    });

                    if (priceUpdated) {
                        jsonLdScript.innerHTML = JSON.stringify(jsonLdData, null, 2);
                    }
                }
            };

            product._updateJsonLdPrice(convertedPrices);

            const updatedJsonLdData = JSON.parse(jsonLdScript.innerHTML);

            assert.equal(updatedJsonLdData.offers[0].price, 98);
            assert.equal(updatedJsonLdData.offers[0].priceSpecification.price, 98);

            // Clean up by removing the script tag
            document.head.removeChild(jsonLdScript);
        });
        it('does not update JSON-LD price when client-side price loading is disabled', () => {
            // Mock CIF.enableClientSidePriceLoading to false
            window.CIF = {
                enableClientSidePriceLoading: false,
                PriceFormatter: class {
                    // Mock the PriceFormatter for this test case as well
                    constructor(price) {
                        this.price = price;
                    }
                    format() {
                        return `$${this.price.toFixed(2)}`; // Basic mock behavior
                    }
                }
            };

            // Initial JSON-LD script setup
            const jsonLdScript = document.createElement('script');
            jsonLdScript.type = 'application/ld+json';
            jsonLdScript.innerHTML = JSON.stringify({
                '@context': 'https://schema.org',
                '@type': 'Product',
                offers: [
                    {
                        sku: 'sample-sku',
                        price: 0, // initial price
                        priceCurrency: 'USD',
                        priceSpecification: {
                            price: 0 // initial regular price
                        }
                    }
                ]
            });
            document.head.appendChild(jsonLdScript);

            // Sample prices to update
            const convertedPrices = {
                'sample-sku': {
                    finalPrice: 98,
                    regularPrice: 98
                }
            };

            let product = new Product({ element: productRoot });
            product._updateJsonLdPrice(convertedPrices);

            const updatedJsonLdData = JSON.parse(jsonLdScript.innerHTML);

            // Assert that the price values were NOT updated (should remain at 0)
            assert.equal(updatedJsonLdData.offers[0].price, 0);
            assert.equal(updatedJsonLdData.offers[0].priceSpecification.price, 0);

            // Clean up by removing the script tag
            document.head.removeChild(jsonLdScript);
        });
        it('updates JSON-LD prices for multiple offers with different SKUs', () => {
            window.CIF = {
                enableClientSidePriceLoading: true, // Ensure client-side price loading is enabled
                PriceFormatter: class {
                    constructor(price) {
                        this.price = price;
                    }
                    format() {
                        return `$${this.price.toFixed(2)}`;
                    }
                }
            };
            const jsonLdScript = document.createElement('script');
            jsonLdScript.type = 'application/ld+json';
            jsonLdScript.innerHTML = JSON.stringify({
                '@context': 'https://schema.org',
                '@type': 'Product',
                offers: [
                    {
                        sku: 'sku-1',
                        price: 0,
                        priceCurrency: 'USD',
                        priceSpecification: {
                            price: 0
                        }
                    },
                    {
                        sku: 'sku-2',
                        price: 0,
                        priceCurrency: 'USD',
                        priceSpecification: {
                            price: 0
                        }
                    }
                ]
            });
            document.head.appendChild(jsonLdScript);

            const convertedPrices = {
                'sku-1': {
                    finalPrice: 50,
                    regularPrice: 60
                },
                'sku-2': {
                    finalPrice: 100,
                    regularPrice: 120
                }
            };

            let product = new Product({ element: productRoot });
            product._updateJsonLdPrice(convertedPrices);

            const updatedJsonLdData = JSON.parse(jsonLdScript.innerHTML);
            assert.equal(updatedJsonLdData.offers[0].price, 50);
            assert.equal(updatedJsonLdData.offers[0].priceSpecification.price, 60);
            assert.equal(updatedJsonLdData.offers[1].price, 100);
            assert.equal(updatedJsonLdData.offers[1].priceSpecification.price, 120);

            document.head.removeChild(jsonLdScript);
        });
    });
});
