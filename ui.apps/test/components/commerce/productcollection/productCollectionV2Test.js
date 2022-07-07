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

import ProductCollection from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productcollection/v2/productcollection/clientlibs/js/productcollection.js';
import ProductCollectionActions from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productcollection/v2/productcollection/clientlibs/js/actions.js';
import PriceFormatter from '../../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/PriceFormatter.js';

describe('Productcollection', () => {
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
        window.CIF.PriceFormatter = PriceFormatter;
    });

    after(() => {
        // Restore original context
        window.CIF = windowCIF;
    });

    beforeEach(() => {
        listRoot = document.createElement('div');
        listRoot.dataset.locale = 'en-US'; // enforce the locale for prices
        listRoot.insertAdjacentHTML(
            'afterbegin',
            `
            <div class=" productcollection__root">
                <div class="productcollection__filters">
                    <div class="productcollection__filters-header">
                        <h5 class="productcollection__filters-title">Filter by</h5>
                    </div>
                    <ul class="productcollection__filters-body">
                        <li class="productcollection__filter">
                            <input type="radio" id="price" name="activeFilter" class="productcollection__filter-toggler">
                            <label for="price" class="productcollection__filter-header">
                                <span class="productcollection__filter-title">Price</span>
                            </label>
                            <ul class="productcollection__filter-items">
                                <li>
                                    <a class="productcollection__filter-item" href="/content/venia/us/en/products/category-page.html/venia-bottoms.html?price=0_100">
                                        0-100
                                        <em> (19)</em>
                                    </a>
                                </li>                            
                                <li>
                                    <a class="productcollection__filter-item" href="/content/venia/us/en/products/category-page.html/venia-bottoms.html?price=100_200">
                                        100-200
                                        <em> (5)</em>
                                    </a>
                                </li>
                            </ul>
                        </li>
                        <li class="productcollection__filter">
                            <input type="radio" id="fashion_color" name="activeFilter" class="productcollection__filter-toggler">
                            <label for="fashion_color" class="productcollection__filter-header">
                                <span class="productcollection__filter-title">Fashion Color</span>
                            </label>
                            <ul class="productcollection__filter-items">
                                <li>
                                    <a class="productcollection__filter-item" href="/content/venia/us/en/products/category-page.html/venia-bottoms.html?fashion_color=14">
                                        Gold
                                        <em> (24)</em>
                                    </a>
                                </li>                            
                                <li>
                                    <a class="productcollection__filter-item" href="/content/venia/us/en/products/category-page.html/venia-bottoms.html?fashion_color=24">
                                        Rain
                                        <em> (21)</em>
                                    </a>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>
                <div class="productcollection__items">
                    <a class="productcollection__item" data-sku="sku-a" role="product">
                        <div class="price">
                            <span>123</span>
                        </div>
                        <div class="productcollection__item-actions">
                            <button data-action="add-to-cart" data-item-sku="sku-a" class="productcollection__item-button productcollection__item-button--add-to-cart" type="button">
                                <span class="productcollection__item-button-content">
                                    <span>Add to Cart</span>
                                </span>
                            </button>
                            <button data-item-sku="sku-a" class="productcollection__item-button productcollection__item-button--add-to-wish-list" type="button" data-cmp-is="add-to-wish-list">
                                <span class="productcollection__item-button-content">
                                    <span>Add to Wish List</span>
                                </span>
                            </button>
                        </div>
                    </a>
                    <a class="productcollection__item" data-sku="sku-b" role="product">
                        <div class="price">
                            <span>456</span>
                        </div>
                        <div class="productcollection__item-actions">
                            <button data-action="details" data-item-sku="sku-b" class="productcollection__item-button productcollection__item-button--add-to-cart" type="button">
                                <span class="productcollection__item-button-content">
                                    <span>Add to Cart</span>
                                </span>
                            </button>
                        </div>
                    </a>
                    <a class="productcollection__item" data-sku="sku-c" role="product">
                        <div class="price">
                            <span>789</span>
                        </div>
                    </a>
                    <a class="productcollection__item" data-sku="sku-d" role="product">
                        <div class="price">
                            <span>101112</span>
                        </div>
                    </a>
                </div>
            </div>`
        );
        document.body.appendChild(listRoot);

        window.CIF.CommerceGraphqlApi = {
            getProductPrices: sinon.stub().resolves(clientPrices)
        };
    });

    afterEach(() => {
        document.body.childNodes.forEach(node => node.remove());
    });

    it('initializes a product list component', () => {
        let list = new ProductCollection({ element: listRoot });

        assert.deepEqual(list._state.skus, ['sku-a', 'sku-b', 'sku-c', 'sku-d']);
    });

    it('retrieves prices via GraphQL', () => {
        listRoot.dataset.loadClientPrice = true;
        let list = new ProductCollection({ element: listRoot });
        assert.isTrue(list._state.loadPrices);

        return list._fetchPrices().then(() => {
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);
            assert.deepEqual(list._state.prices, convertedPrices);

            // Verify price updates
            assert.equal(listRoot.querySelector('[data-sku=sku-a] .price').innerText, '$156.89');
            assert.equal(listRoot.querySelector('[data-sku=sku-b] .price').innerText, 'From $123.45 To $150.45');
            assert.include(listRoot.querySelector('[data-sku=sku-c] .price').innerText, '$20.00');
            assert.include(listRoot.querySelector('[data-sku=sku-c] .price').innerText, '$10.00');
            assert.equal(listRoot.querySelector('[data-sku=sku-d] .price').innerText, 'Starting at $20.00');
        });
    });

    it('displays a null price', () => {
        listRoot = document.createElement('div');
        listRoot.dataset.locale = 'en-US'; // enforce the locale for prices
        listRoot.insertAdjacentHTML(
            'afterbegin',
            `
            <div class="gallery__items">
                <div class="productcollection__item" data-sku="sku-a" role="product"></div>
                <div class="productcollection__item" data-sku="sku-b" role="product"></div>
                <div class="productcollection__item" data-sku="sku-c" role="product"></div>
                <div class="productcollection__item" data-sku="sku-d" role="product"></div>
            </div>`
        );

        const priceRange = {
            'sku-a': {
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
            },
            'sku-b': {
                minimum_price: {
                    regular_price: {
                        value: null,
                        currency: 'USD'
                    },
                    final_price: {
                        value: null,
                        currency: 'USD'
                    }
                },
                maximum_price: {
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

        listRoot.dataset.loadClientPrice = true;
        let list = new ProductCollection({ element: listRoot });
        assert.isTrue(list._state.loadPrices);

        return list._fetchPrices().then(() => {
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.called);

            // Verify price updates
            assert.equal(listRoot.querySelector('[data-sku=sku-a] .price'), null);
            assert.equal(listRoot.querySelector('[data-sku=sku-b] .price'), null);
            assert.equal(listRoot.querySelector('[data-sku=sku-c] .price'), null);
            assert.equal(listRoot.querySelector('[data-sku=sku-c] .price'), null);
        });
    });

    it('skips retrieving of prices if CommerceGraphqlApi is not available', () => {
        delete window.CIF.CommerceGraphqlApi;

        listRoot.dataset.loadClientPrice = true;
        let list = new ProductCollection({ element: listRoot });
        assert.isTrue(list._state.loadPrices);

        list._fetchPrices();
        assert.isEmpty(list._state.prices);
    });

    it('skips retrieving of prices via GraphQL when data attribute is not set', () => {
        let list = new ProductCollection({ element: listRoot });
        assert.isFalse(list._state.loadPrices);
    });

    it('lazy loads products', () => {
        listRoot.insertAdjacentHTML(
            'beforeend',
            `<button class="productcollection__loadmore-button" data-load-more="http://more.products">Load more</button>
            <div class="productcollection__loadmore-spinner"></div>`
        );
        listRoot.dataset.loadClientPrice = true;

        let response = `
            <div class="productcollection__item" data-sku="sku-e" role="product">
                <div class="price">
                    <span>123</span>
                </div>
            </div>
            <button class="productcollection__loadmore-button" data-load-more="http://more.products2">Load more</button>`;

        let mockResponse = new window.Response(response, {
            status: 200,
            headers: {
                'Content-type': 'text/html'
            }
        });

        let list = new ProductCollection({ element: listRoot });

        // Check the skus before we load more products
        assert.deepEqual(list._state.skus, ['sku-a', 'sku-b', 'sku-c', 'sku-d']);

        list._fetchMoreProducts = sinon.stub().resolves(mockResponse);
        let loadMoreButton = listRoot.querySelector('.productcollection__loadmore-button');

        return list._loadMore(loadMoreButton).then(() => {
            assert.isTrue(list._fetchMoreProducts.called);

            // Verify that the product has been added to the HTML
            assert.equal(listRoot.querySelectorAll('.productcollection__item').length, 5);

            // Verify that the first load more button was replaced with the new button
            assert.equal(
                listRoot.querySelector('.productcollection__loadmore-button').dataset.loadMore,
                'http://more.products2'
            );

            // Check the skus after we load more products and check that the price loading function was called twice
            assert.deepEqual(list._state.skus, ['sku-e']);
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.calledTwice);
        });
    });

    it('lazy loads products with HTTP error', () => {
        listRoot.insertAdjacentHTML(
            'beforeend',
            `<button class="productcollection__loadmore-button" data-load-more="http://more.products">Load more</button>
            <div class="productcollection__loadmore-spinner"></div>`
        );

        let mockResponse = new window.Response('Internal server error', {
            status: 500,
            headers: {
                'Content-type': 'text/html'
            }
        });

        let list = new ProductCollection({ element: listRoot });
        list._fetchMoreProducts = sinon.stub().resolves(mockResponse);
        let loadMoreButton = listRoot.querySelector('.productcollection__loadmore-button');

        return list._loadMore(loadMoreButton).catch(error => {
            assert.isTrue(list._fetchMoreProducts.called);
            assert.equal(error.message, 'Internal server error');

            // Verify that the product has NOT been added to the HTML
            assert.equal(listRoot.querySelectorAll('.productcollection__item').length, 4);

            // Verify that the first load more button is still there
            assert.equal(
                listRoot.querySelector('.productcollection__loadmore-button').dataset.loadMore,
                'http://more.products'
            );
        });
    });

    it('selects and deselects filter on mouse click', () => {
        let list = new ProductCollection({ element: listRoot });

        let priceFilter = listRoot.querySelector('.productcollection__filter-toggler[id="price"]');
        assert.isNotNull(priceFilter);
        assert.isFalse(priceFilter.checked);

        priceFilter.click();
        assert.isTrue(priceFilter.checked);

        priceFilter.click();
        assert.isFalse(priceFilter.checked);
    });

    it('triggers the add-to-cart event for the Add to Cart button add-to-cart call to action', () => {
        new ProductCollectionActions(listRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-cart', spy);
        const button = listRoot.querySelector('button.productcollection__item-button--add-to-cart');

        button.click();

        assert.isTrue(spy.calledOnce);
    });

    it('propagates the click to the parent link for the Add to Cart button details call to action', () => {
        new ProductCollectionActions(listRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-cart', spy);
        const spyLink = sinon.spy();
        const button = listRoot.querySelector(
            'button.productcollection__item-button--add-to-cart[data-action="details"]'
        );
        const link = button.closest('a');
        link.addEventListener('click', spyLink);

        button.click();

        assert.isFalse(spy.called);
        assert.isTrue(spyLink.calledOnce);
    });

    it('triggers the add-to-wish-list event for the Add to Wish List button', () => {
        new ProductCollectionActions(listRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-wishlist', spy);
        const button = listRoot.querySelector('button.productcollection__item-button--add-to-wish-list');

        button.click();

        assert.isTrue(spy.calledOnce);
    });
});
