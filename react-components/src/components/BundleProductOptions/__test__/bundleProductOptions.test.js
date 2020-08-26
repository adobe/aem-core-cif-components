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
import { render } from '@testing-library/react';
import * as ConfigContext from '../../../context/ConfigContext';

import i18n from '../../../../__mocks__/i18nForTests';

import BundleProductOptions from '../bundleProductOptions';

describe('<BundleProductOptions>', () => {
    beforeAll(() => {
        // mock useConfigContext to return the necessary node selector
        jest.spyOn(ConfigContext, 'useConfigContext').mockImplementation(() => {
            return {
                mountingPoints: { bundleProductOptionsContainer: '#bundle-product-options' }
            };
        });
    });

    it('renders the component with no sku', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <BundleProductOptions />
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with sku', () => {
        const container = document.createElement('div');
        container.dataset.sku = 'VA-42';
        container.id = 'bundle-product-options';
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <BundleProductOptions />
                </MockedProvider>
            </I18nextProvider>,
            {
                container: document.body.appendChild(container)
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
});
