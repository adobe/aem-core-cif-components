/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import { fireEvent, waitForDomChange, wait } from '@testing-library/react';
import { render } from 'test-utils';
import * as hooks from '../../../utils/hooks';
import BundleProductOptions from '../bundleProductOptions';
import mockResponse from './graphQlMockReponse';
import mockAddToCartMutation from './graphqlMockAddToCartMutation';

const config = {
    storeView: 'default',
    graphqlEndpoint: 'endpoint',
    graphqlMethod: 'GET',
    mountingPoints: {
        bundleProductOptionsContainer: '#bundle-product-options'
    }
};

describe('BundleProductOptions', () => {
    it('renders the component with no sku', () => {
        const { asFragment } = render(<BundleProductOptions />, { config: config });
        expect(asFragment()).toMatchInlineSnapshot(`<DocumentFragment />`);
    });

    it('renders the component with sku', () => {
        const bundleProductOptionsContainer = document.createElement('div');
        bundleProductOptionsContainer.dataset.sku = 'VA24';
        bundleProductOptionsContainer.id = 'bundle-product-options';
        const { asFragment } = render(<BundleProductOptions />, {
            config: config,
            container: document.body.appendChild(bundleProductOptionsContainer)
        });

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with auto load options', async () => {
        // mock useState to return the state for a full rendering
        jest.spyOn(hooks, 'useAwaitQuery').mockImplementation(() => {
            return jest.fn().mockImplementation(async () => {
                return {
                    data: mockResponse,
                    error: null
                };
            });
        });

        const bundleProductOptionsContainer = document.createElement('div');
        bundleProductOptionsContainer.dataset.sku = 'VA24';
        bundleProductOptionsContainer.id = 'bundle-product-options';
        const { asFragment, container } = render(<BundleProductOptions autoLoadOptions={true} />, {
            config: config,
            container: document.body.appendChild(bundleProductOptionsContainer)
        });

        // no content
        expect(asFragment()).toMatchSnapshot();

        await waitForDomChange({ container });

        // with content
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with full options', async () => {
        // mock useState to return the state for a full rendering
        jest.spyOn(hooks, 'useAwaitQuery').mockImplementation(() => {
            return jest.fn().mockImplementation(async () => {
                return {
                    data: mockResponse,
                    error: null
                };
            });
        });

        const eventHandler = jest.fn().mockImplementation(event => {
            return event.detail;
        });
        document.addEventListener('aem.cif.add-to-cart', eventHandler);

        const bundleProductOptionsContainer = document.createElement('div');
        bundleProductOptionsContainer.dataset.sku = 'VA24';
        bundleProductOptionsContainer.dataset.showQuantity = true;
        bundleProductOptionsContainer.id = 'bundle-product-options';

        const { asFragment, container, getByRole } = render(<BundleProductOptions />, {
            config: config,
            container: document.body.appendChild(bundleProductOptionsContainer),
            mocks: [mockAddToCartMutation]
        });

        fireEvent.click(getByRole('button', { name: 'Customize' }));

        await waitForDomChange({ container });

        expect(asFragment()).toMatchSnapshot();

        fireEvent.click(getByRole('button', { name: 'Add to Cart' }));

        // Remove a required item to disable Add to Cart
        fireEvent.click(getByRole('checkbox', { name: '1 x Carmina Necklace + $78.00' }));
        fireEvent.click(getByRole('button', { name: 'Add to Cart' }));

        // Add to cart should be called just once since the second click was on a disabled button
        await wait(() => expect(eventHandler).toHaveBeenCalledTimes(1));

        // The mock dispatchEvent function returns the CustomEvent detail
        expect(eventHandler).toHaveReturnedWith([
            {
                bundle: true,
                options: [
                    { id: 1, quantity: 1, value: ['1'] },
                    { id: 2, quantity: 1, value: ['3'] },
                    { id: 3, quantity: 1, value: ['5'] },
                    { id: 4, quantity: 1, value: ['7', '8'] }
                ],
                parentSku: 'VA24',
                quantity: 1,
                selected_options: [
                    'YnVuZGxlLzEvMS8x',
                    'YnVuZGxlLzIvMy8x',
                    'YnVuZGxlLzMvNS8x',
                    'YnVuZGxlLzQvNy8x',
                    'YnVuZGxlLzQvOC8x'
                ],
                sku: 'VA24',
                virtual: false,
                storefrontData: {
                    name: 'Night Out Collection',
                    currencyCode: 'USD',
                    regularPrice: 380,
                    finalPrice: 380,
                    selectedOptions: [
                        { attribute: '1', value: '1' },
                        { attribute: '2', value: '3' },
                        { attribute: '3', value: '5' },
                        { attribute: '4', value: '7,8' }
                    ]
                }
            }
        ]);
    });

    it('renders add to wish list button', async () => {
        // mock useState to return the state for a full rendering
        jest.spyOn(hooks, 'useAwaitQuery').mockImplementation(() => {
            return jest.fn().mockImplementation(async () => {
                return {
                    data: mockResponse,
                    error: null
                };
            });
        });

        const bundleProductOptionsContainer = document.createElement('div');
        bundleProductOptionsContainer.dataset.sku = 'VA24';
        bundleProductOptionsContainer.dataset.showAddToWishList = '';
        bundleProductOptionsContainer.id = 'bundle-product-options';

        const eventHandler = jest.fn().mockImplementation(event => {
            return event.detail;
        });
        document.addEventListener('aem.cif.add-to-wishlist', eventHandler);

        const { asFragment, container, getByRole } = render(<BundleProductOptions />, {
            config: config,
            container: document.body.appendChild(bundleProductOptionsContainer),
            mocks: [mockAddToCartMutation]
        });

        fireEvent.click(getByRole('button', { name: 'Customize' }));

        await waitForDomChange({ container });

        expect(asFragment()).toMatchSnapshot();

        fireEvent.click(getByRole('button', { name: 'Add to Wish List' }));

        await wait(() => expect(eventHandler).toHaveBeenCalledTimes(1));

        // The mock dispatchEvent function returns the CustomEvent detail
        expect(eventHandler).toHaveReturnedWith([
            {
                quantity: 1,
                selected_options: [
                    'YnVuZGxlLzEvMS8x',
                    'YnVuZGxlLzIvMy8x',
                    'YnVuZGxlLzMvNS8x',
                    'YnVuZGxlLzQvNy8x',
                    'YnVuZGxlLzQvOC8x'
                ],
                sku: 'VA24'
            }
        ]);
    });
});
