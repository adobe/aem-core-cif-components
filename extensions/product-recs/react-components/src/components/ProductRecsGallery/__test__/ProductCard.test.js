/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

import React from 'react';

import { fireEvent, render } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';

import ProductCard from '../ProductCard';
import i18n from '../../../../__mocks__/i18nForTests';
import mockMagentoStorefrontEvents from '../../../__test__/mockMagentoStorefrontEvents';

describe('ProductCard', () => {
    let mse;
    let product;
    const unit = {
        unitId: 'my-unit-id'
    };

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

    it('renders a product card', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <ProductCard unit={unit} product={product} />
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders a product card with a price range', () => {
        product.prices.maximum.final = 650.0;
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <ProductCard unit={unit} product={product} />
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('triggers an add to cart event', () => {
        const eventListener = jest.fn();
        document.addEventListener('aem.cif.add-to-cart', eventListener);

        const { queryByRole } = render(
            <I18nextProvider i18n={i18n}>
                <ProductCard unit={unit} product={product} />
            </I18nextProvider>
        );

        fireEvent.click(queryByRole('button'));
        expect(eventListener).toHaveBeenCalledTimes(1);
        expect(mse.publish.recsItemAddToCartClick).toHaveBeenCalledWith(unit.unitId, product.productId);
    });

    it('redirects to the product page', () => {
        const { getByRole } = render(
            <I18nextProvider i18n={i18n}>
                <ProductCard unit={unit} product={product} />
            </I18nextProvider>
        );

        // Check href on link
        expect(getByRole('link').href).toEqual('http://localhost/content/venia/us/en.cifredirect.html/product/my-sku');

        // Click and check MSE
        fireEvent.click(getByRole('link'));
        expect(mse.publish.recsItemClick).toHaveBeenCalledWith(unit.unitId, product.productId);
    });
});
