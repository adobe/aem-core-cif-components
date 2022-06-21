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

import ProductTeaser, {
    LocationAdapter,
    onDocumentReady
} from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/productteaser/v1/productteaser/clientlibs/js/actions';
import CommerceGraphqlApi from '../../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js';

describe('ProductTeaser', () => {
    let pageRoot;
    let teaserRoot;
    let mockLocation;
    let windowCIF;

    let addToCartAction = ({ sku = 1234 } = {}) => `<button data-action="addToCart" data-item-sku="${sku}"
            class="button__root_highPriority button__root clickable__root button__filled" type="button">
        <span class="button__content"><span>Add to Cart</span></span>
        </button>`;

    let seeMoreDetailsAction = ({
        url = '/some/random/url',
        target
    } = {}) => `<button data-action="details" data-url="${url}" ${target ? 'data-target="' + target + '"' : ''}
            class="button__root_highPriority button__root clickable__root button__filled" type="button">
        <span class="button__content"><span>See more details</span></span>
        </button>`;

    let misconfiguredAction = () => `<button data-action="" data-url="/some/random/url"
            class="button__root_highPriority button__root clickable__root button__filled" type="button">
        <span class="button__content"><span>See more details</span></span>
        </button>`;

    let generateTeaserHtml = (
        buttonFn,
        sku = 1234,
        props = {}
    ) => `<div class="item__root" data-cmp-is="productteaser" data-product-sku="${sku}">
        <a class="item__images" href="#"></a>
        <a class="item__name" href="#"><span>Sample product</span></a>
        <div class="price">
        <span> $0.99</span>
        </div>
        <div class="productteaser__cta">
        ${buttonFn({ sku, ...props })}
        <button class="button__root_normalPriority button__root clickable__root" data-item-sku="1234" data-action="wishlist" type="button">
        <span class="button__content">
            <span>Add to Wish List</span>
        </span >
        </button >
        </div>
        </div>`;

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

    before(() => {
        mockLocation = sinon.mock(LocationAdapter);

        let body = document.querySelector('body');
        pageRoot = document.createElement('div');
        body.appendChild(pageRoot);

        // Create empty context
        windowCIF = window.CIF;
        window.CIF = { ...window.CIF };
        window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({ graphqlEndpoint: 'https://foo.bar/graphql' });
        window.CIF.CommerceGraphqlApi.getProductPrices = sinon.stub().resolves(clientPrices);
    });

    beforeEach(() => {
        while (pageRoot.firstChild) {
            pageRoot.removeChild(pageRoot.firstChild);
        }
    });

    after(() => {
        pageRoot.parentNode.removeChild(pageRoot);
        window.CIF = windowCIF;
    });

    it('triggers the cart addition event for the Add To Cart CTA', () => {
        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(addToCartAction));
        teaserRoot = pageRoot.querySelector(ProductTeaser.selectors.rootElement);

        document.addEventListener('aem.cif.add-to-cart', e => {
            let response = document.createElement('div');
            response.classList.add('response');
            response.innerText = JSON.stringify(e.detail);
            pageRoot.appendChild(response);
        });

        const productTeaser = new ProductTeaser(teaserRoot);
        const button = teaserRoot.querySelector('button.button__root_highPriority');
        button.click();

        const response = pageRoot.querySelector('div.response');
        assert.isNotNull(response);
        assert.equal('[{"sku":"1234","quantity":1,"virtual":false}]', response.innerText);
    });

    it('triggers the wishlist addition event for the Add To Wishlist CTA', () => {
        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(addToCartAction));
        teaserRoot = pageRoot.querySelector(ProductTeaser.selectors.rootElement);

        document.addEventListener('aem.cif.add-to-wishlist', e => {
            let response = document.createElement('div');
            response.classList.add('response');
            response.innerText = JSON.stringify(e.detail);
            pageRoot.appendChild(response);
        });

        const productTeaser = new ProductTeaser(teaserRoot);
        const button = teaserRoot.querySelector('button.button__root_normalPriority');
        button.click();

        const response = pageRoot.querySelector('div.response');
        assert.isNotNull(response);
        assert.equal('[{"sku":"1234","quantity":1}]', response.innerText);
    });

    it('navigates to another location for the See Details CTA', () => {
        mockLocation
            .expects('setHref')
            .atLeast(1)
            .withArgs('/some/random/url');

        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(seeMoreDetailsAction));
        teaserRoot = pageRoot.querySelector(ProductTeaser.selectors.rootElement);

        const productTeaser = new ProductTeaser(teaserRoot);
        const button = teaserRoot.querySelector('button.button__root_highPriority');
        button.click();
        mockLocation.verify();
    });

    it('opens another location for the See Details CTA with link target specified', () => {
        mockLocation
            .expects('openHref')
            .atLeast(1)
            .withArgs('/some/random/url', '_blank');

        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(seeMoreDetailsAction, 1234, { target: '_blank' }));
        teaserRoot = pageRoot.querySelector(ProductTeaser.selectors.rootElement);

        const productTeaser = new ProductTeaser(teaserRoot);
        const button = teaserRoot.querySelector('button.button__root_highPriority');
        button.click();
        mockLocation.verify();
    });

    it("doesn't do anything if the CTA is misconfigured", () => {
        const eventListener = sinon.spy();
        mockLocation.expects('openHref').never();
        document.addEventListener('aem.cif.add-to-wishlist', eventListener);
        document.addEventListener('aem.cif.add-to-cart', eventListener);

        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(misconfiguredAction));
        teaserRoot = pageRoot.querySelector(ProductTeaser.selectors.rootElement);
        const productTeaser = new ProductTeaser(teaserRoot);
        const button = teaserRoot.querySelector('button.button__root_highPriority');
        button.click();

        mockLocation.verify();
        assert(eventListener.notCalled);
    });

    it('retrieves prices via GraphQL', () => {
        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(addToCartAction, 'sku-a'));
        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(addToCartAction, 'sku-c'));
        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(addToCartAction, 'sku-b'));
        pageRoot.insertAdjacentHTML('afterbegin', generateTeaserHtml(addToCartAction, 'sku-d'));
        pageRoot.querySelectorAll(ProductTeaser.selectors.rootElement).forEach(teaserRoot => {
            teaserRoot.dataset.locale = 'en';
            teaserRoot.dataset.loadPrice = true;
        });

        // dispatch the DOMContentLoaded event again
        onDocumentReady(window.document);
        assert.isNotNull(ProductTeaser.prices$);

        return ProductTeaser.prices$.then(() => {
            // all skus are queried at once
            assert.isTrue(window.CIF.CommerceGraphqlApi.getProductPrices.calledOnce);

            // verify price updates
            assert.equal(pageRoot.querySelector('[data-product-sku="sku-a"] .price').innerText, '$156.89');
            assert.equal(
                pageRoot.querySelector('[data-product-sku="sku-b"] .price').innerText,
                'From $123.45 To $150.45'
            );
            assert.include(pageRoot.querySelector('[data-product-sku="sku-c"] .price').innerText, '$20.00');
            assert.include(pageRoot.querySelector('[data-product-sku="sku-c"] .price').innerText, '$10.00');
            assert.equal(pageRoot.querySelector('[data-product-sku="sku-d"] .price').innerText, '$20.00');
        });
    });
});
