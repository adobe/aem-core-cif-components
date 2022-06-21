/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import ProductCarouselActions from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs/js/actions';
import CommerceGraphqlApi from '../../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js';
import {
    ProductCarousel,
    onDocumentReady
} from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productcarousel/v1/productcarousel/clientlibs/js/product-carousel';

describe('ProductCarousel', () => {
    let body;
    let carouselRoot;
    let windowCIF;

    const newProductCarousel = ({
        cards = [
            {
                title: 'Card 1',
                sku: 'sku-a',
                addToWishList: true
            },
            {
                title: 'Card 2',
                sku: 'sku-c',
                addToCartAction: 'details'
            }
        ]
    } = {}) =>
        `
        <div class="productcarousel">
            <div data-comp-is="productcarousel" class="productcarousel__container" data-locale="en">
                <button data-carousel-action="prev" class="productcarousel__btn productcarousel__btn--prev" type="button" title="Show previous" aria-label="Show previous" style="display: none;"></button>
                <button data-carousel-action="next" class="productcarousel__btn productcarousel__btn--next" type="button" title="Show next" aria-label="Show next" style="display: none;"></button>
                <div class="productcarousel__root">
                    <div class="productcarousel__parent">
                        <div class="productcarousel__cardscontainer">
                            ${cards.map(
                                ({
                                    title,
                                    sku,
                                    baseSku = sku,
                                    addToCartAction = 'add-to-cart',
                                    addToWishList = false
                                }) => `
                                <div class="card product__card" data-product-sku="${sku}" data-product-base-sku="${baseSku}">
                                    <a class="product-card-content">
                                        <div class="product__card-title">${title}</div>
                                        <div class="product__card-actions">
                                            <button data-action="${addToCartAction}" data-item-sku="${sku}" class="product__card-button product__card-button--add-to-cart"/>
                                            ${addToWishList &&
                                                `<button data-item-sku="${sku}" class="product__card-button product__card-button--add-to-wish-list"/>`}
                                        </div>
                                        <div class="price"></div>
                                    </a>
                                </div>
                            `
                            )}                                 
                        </div>                                
                    </div>
                </div>
            </div>
        </div>
        `;

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
        'sku-b-variant-b': {
            __typename: 'SimpleProduct',
            minimum_price: {
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
        },
        'no-price': {
            __typename: 'SimpleProduct'
        }
    };

    before(() => {
        body = window.document.querySelector('body');
        carouselRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', carouselRoot);
        carouselRoot.insertAdjacentHTML('afterbegin', newProductCarousel());

        // Create empty context
        windowCIF = window.CIF;
        window.CIF = { ...window.CIF };
        window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({ graphqlEndpoint: 'https://foo.bar/graphql' });
        window.CIF.CommerceGraphqlApi.getProductPrices = sinon.stub().resolves(clientPrices);
    });

    after(() => {
        body.removeChild(carouselRoot);
        window.CIF = windowCIF;
    });

    it('triggers the add-to-cart event for the Add to Cart button add-to-cart call to action', () => {
        new ProductCarouselActions(carouselRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-cart', spy);
        const button = carouselRoot.querySelector('button.product__card-button--add-to-cart');

        button.click();

        assert.isTrue(spy.called);
    });

    it('propagates the click to the parent link for the Add to Cart button details call to action', () => {
        new ProductCarouselActions(carouselRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-cart', spy);
        const spyLink = sinon.spy();
        const button = carouselRoot.querySelector('button.product__card-button--add-to-cart[data-action="details"]');
        const link = button.closest('a');
        link.addEventListener('click', spyLink);

        button.click();

        assert.isFalse(spy.called);
        assert.isTrue(spyLink.called);
    });

    it('triggers the add-to-wish-list event for the Add to Wish List button', () => {
        new ProductCarouselActions(carouselRoot);

        const spy = sinon.spy();
        document.addEventListener('aem.cif.add-to-wishlist', spy);
        const button = carouselRoot.querySelector('button.product__card-button--add-to-wish-list');

        button.click();

        assert.isTrue(spy.called);
    });

    it('retrieves prices via GraphQL', () => {
        carouselRoot.insertAdjacentHTML(
            'afterbegin',
            newProductCarousel({
                cards: [
                    {
                        title: 'Card 3',
                        sku: 'sku-d'
                    },
                    {
                        title: 'Card 4',
                        baseSku: 'sku-b',
                        sku: 'sku-b-variant-b'
                    },
                    {
                        title: 'Card 5',
                        sku: 'unknown'
                    },
                    {
                        title: 'Card 6',
                        sku: 'no-price'
                    }
                ]
            })
        );

        carouselRoot.querySelectorAll(ProductCarousel.selectors.self).forEach(carousel => {
            carousel.dataset.loadPrices = true;
        });

        // dispatch the DOMContentLoaded event again
        onDocumentReady(window.document);
        assert.isNotNull(ProductCarousel.prices$);

        return ProductCarousel.prices$.then(() => {
            // all skus are queried at once
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.calledOnce);
            sinon.assert.calledWith(
                window.CIF.CommerceGraphqlApi.getProductPrices,
                ['sku-d', 'sku-b', 'unknown', 'no-price', 'sku-a', 'sku-c'],
                true
            );

            // verify price updates
            assert.equal(carouselRoot.querySelector('[data-product-sku="sku-a"] .price').innerText, '$156.89');
            assert.equal(
                carouselRoot.querySelector('[data-product-sku="sku-b-variant-b"] .price').innerText,
                '$150.45'
            );
            assert.include(carouselRoot.querySelector('[data-product-sku="sku-c"] .price').innerText, '$20.00');
            assert.include(carouselRoot.querySelector('[data-product-sku="sku-c"] .price').innerText, '$10.00');
            assert.equal(
                carouselRoot.querySelector('[data-product-sku="sku-d"] .price').innerText,
                'Starting at $20.00'
            );
            assert.equal(carouselRoot.querySelector('[data-product-sku="unknown"] .price').innerText, '');
            assert.equal(carouselRoot.querySelector('[data-product-sku="no-price"] .price').innerText, '');
        });
    });
});
