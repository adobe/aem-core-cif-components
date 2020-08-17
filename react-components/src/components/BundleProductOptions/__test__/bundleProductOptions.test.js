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
import ReactDOM from 'react-dom';
import { I18nextProvider } from 'react-i18next';
import { MockedProvider } from '@apollo/react-testing';
import { render } from '@testing-library/react';

import i18n from '../../../../__mocks__/i18nForTests';

import BundleProductOptions from '../bundleProductOptions';

describe('<BundleProductOptions>', () => {
    beforeAll(() => {
        // mock createPortal because we don't have the DOM element to render the BundleProductOptions
        jest.spyOn(ReactDOM, 'createPortal').mockImplementation(element => element);
    });

    it('renders the component', () => {
        const container = {
            querySelector: jest.fn(() => {
                return { dataset: { sku: 'VA24' } };
            })
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <BundleProductOptions container={container} />
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
