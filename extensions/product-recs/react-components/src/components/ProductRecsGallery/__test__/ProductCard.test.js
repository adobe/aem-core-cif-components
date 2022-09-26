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
import React from 'react';

import { fireEvent, render } from '@testing-library/react';

import ProductCard from '../ProductCard';
import mockMagentoStorefrontEvents from '../../../__test__/mockMagentoStorefrontEvents';
import ContextWrapper from '../../../__test__/context-wrapper';
import { StorefrontInstanceContext } from '../../../context/StorefrontInstanceContext';

describe('ProductCard', () => {
    let mse;
    let product;
    const unit = {
        unitId: 'my-unit-id'
    };

    const TestWrapper = ({ children }) => (
        <ContextWrapper>
            <StorefrontInstanceContext.Provider value={{ mse }}>{children}</StorefrontInstanceContext.Provider>
        </ContextWrapper>
    );

    beforeAll(() => {
        document.body.dataset.storeRootUrl = '/content/venia/us/en.html';
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;
    });

    beforeEach(() => {
        window.magentoStorefrontEvents.mockClear();
        product = {
            sku: 'my-sku',
            name: 'My Product',
            type: 'simple',
            productId: 123,
            currency: 'CHF',
            prices: {
                minimum: {
                    final: 342.23
                },
                maximum: {
                    final: 342.23
                }
            },
            smallImage: {
                url: 'http://localhost/product.png'
            }
        };
    });

    it.each([
        ['with add to wish list', true],
        ['without add to wish list', undefined]
    ])('renders a product card (%s)', (_name, showAddToWishList) => {
        const { asFragment } = render(
            <ProductCard unit={unit} product={product} showAddToWishList={showAddToWishList} />,
            { wrapper: TestWrapper }
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders a product card with a price range', () => {
        product.prices.maximum.final = 650.0;
        const { asFragment } = render(<ProductCard unit={unit} product={product} />, { wrapper: TestWrapper });

        expect(asFragment()).toMatchSnapshot();
    });

    it('triggers an add to cart event', () => {
        const eventListener = jest.fn();
        document.addEventListener('aem.cif.add-to-cart', eventListener);

        const { queryByRole } = render(<ProductCard unit={unit} product={product} />, { wrapper: TestWrapper });

        fireEvent.click(
            queryByRole('button', {
                name: /add to cart/i
            })
        );
        expect(eventListener).toHaveBeenCalledTimes(1);
        expect(mse.publish.recsItemAddToCartClick).toHaveBeenCalledWith(unit.unitId, product.productId);
    });

    it('triggers an add to wish list event', () => {
        const eventListener = jest.fn();
        document.addEventListener('aem.cif.add-to-wishlist', eventListener);

        const { queryByRole } = render(<ProductCard unit={unit} product={product} showAddToWishList={true} />, {
            wrapper: TestWrapper
        });

        fireEvent.click(
            queryByRole('button', {
                name: /add to Wish List/i
            })
        );
        expect(eventListener).toHaveBeenCalledTimes(1);
    });

    it('button redirects to the product page', () => {
        product = {
            sku: 'my-sku',
            name: 'My Product',
            type: 'configurable',
            productId: 123,
            currency: 'CHF',
            prices: {
                minimum: {
                    final: 342.23
                },
                maximum: {
                    final: 342.23
                }
            },
            smallImage: {
                url: 'http://localhost/product.png'
            }
        };

        const { queryByRole } = render(<ProductCard unit={unit} product={product} />, { wrapper: TestWrapper });

        fireEvent.click(queryByRole('button', { name: 'Add to Cart' }));
        expect(mse.publish.recsItemClick).toHaveBeenCalledWith(unit.unitId, product.productId);
    });

    it('redirects to the product page', () => {
        const { getByRole } = render(<ProductCard unit={unit} product={product} />, { wrapper: TestWrapper });

        // Check href on link
        expect(getByRole('link').href).toEqual('http://localhost/content/venia/us/en.cifproductredirect.html/my-sku');

        // Click and check MSE
        fireEvent.click(getByRole('link'));
        expect(mse.publish.recsItemClick).toHaveBeenCalledWith(unit.unitId, product.productId);
    });

    it('renders a product without image', () => {
        product.smallImage = null;
        const { asFragment } = render(<ProductCard unit={unit} product={product} />, { wrapper: TestWrapper });

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders a product without price', () => {
        product.prices = null;
        const { asFragment } = render(<ProductCard unit={unit} product={product} />, { wrapper: TestWrapper });

        expect(asFragment()).toMatchSnapshot();
    });
});
