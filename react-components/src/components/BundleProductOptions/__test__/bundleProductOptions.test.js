/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import { I18nextProvider } from 'react-i18next';
import { MockedProvider } from '@apollo/react-testing';
import { render, fireEvent, waitForDomChange } from '@testing-library/react';
import ConfigContext from '../../../context/ConfigContext';
import * as hooks from '../../../utils/hooks';

import i18n from '../../../../__mocks__/i18nForTests';

import BundleProductOptions from '../bundleProductOptions';
import mockResponse from './graphQlMockReponse';

const config = { mountingPoints: { bundleProductOptionsContainer: '#bundle-product-options' } };

describe('<BundleProductOptions>', () => {
    it('renders the component with no sku', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <ConfigContext config={config}>
                        <BundleProductOptions />
                    </ConfigContext>
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchInlineSnapshot(`<DocumentFragment />`);
    });

    it('renders the component with sku', () => {
        const bundleProductOptionsContainer = document.createElement('div');
        bundleProductOptionsContainer.dataset.sku = 'VA24';
        bundleProductOptionsContainer.id = 'bundle-product-options';
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <ConfigContext config={config}>
                        <BundleProductOptions />
                    </ConfigContext>
                </MockedProvider>
            </I18nextProvider>,
            {
                container: document.body.appendChild(bundleProductOptionsContainer)
            }
        );

        expect(asFragment()).toMatchInlineSnapshot(`
            <DocumentFragment>
              <section
                class="productFullDetail__section productFullDetail__customizeBundle"
              >
                <button
                  class="root_highPriority"
                  type="button"
                >
                  <span
                    class="content"
                  >
                    <span>
                      Customize
                    </span>
                  </span>
                </button>
              </section>
            </DocumentFragment>
        `);
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

        const dispatchEventSpy = jest.spyOn(document, 'dispatchEvent').mockImplementation(event => {
            return event.detail;
        });

        const bundleProductOptionsContainer = document.createElement('div');
        bundleProductOptionsContainer.dataset.sku = 'VA24';
        bundleProductOptionsContainer.id = 'bundle-product-options';

        const { container, getByRole } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <ConfigContext config={config}>
                        <BundleProductOptions />
                    </ConfigContext>
                </MockedProvider>
            </I18nextProvider>,
            {
                container: document.body.appendChild(bundleProductOptionsContainer)
            }
        );

        fireEvent.click(getByRole('button', { name: 'Customize' }));

        await waitForDomChange({ container });
        expect(container).toMatchSnapshot();

        fireEvent.click(getByRole('button', { name: 'Add to Cart' }));

        // Remove a required item to disable Add to Cart
        fireEvent.click(getByRole('checkbox', { name: '1 x Carmina Necklace + $78.00' }));
        fireEvent.click(getByRole('button', { name: 'Add to Cart' }));

        // Add to cart should be called just once since the second click was on a disabled button
        expect(dispatchEventSpy).toHaveBeenCalledTimes(1);

        // The mock dispatchEvent function returns the CustomEvent detail
        expect(dispatchEventSpy).toHaveReturnedWith([
            {
                bundle: true,
                options: [
                    { id: 1, quantity: 1, value: ['1'] },
                    { id: 2, quantity: 1, value: ['3'] },
                    { id: 3, quantity: 1, value: ['5'] },
                    { id: 4, quantity: 1, value: ['7', '8'] }
                ],
                quantity: 1,
                sku: 'VA24',
                virtual: false
            }
        ]);
    });
});
