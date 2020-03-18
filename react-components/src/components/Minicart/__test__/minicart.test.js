/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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
import { render } from 'test-utils';
import { I18nextProvider } from 'react-i18next';

import Minicart from '../minicart';
import { waitForElement } from '@testing-library/dom';
import { CartProvider } from '../cartContext';
import i18n from '../../../../__mocks__/i18nForTests';

describe('<Minicart>', () => {
    beforeAll(() => {
        // mock createPortal because we don't have the DOM element to render the CartTrigger
        ReactDOM.createPortal = jest.fn(element => {
            return element;
        });
    });

    it('renders the empty cart', async () => {
        const { getByTestId } = render(
            <I18nextProvider i18n={i18n}>
                <CartProvider initialState={{ cartId: 'empty' }} reducerFactory={() => state => state}>
                    <Minicart />
                </CartProvider>
            </I18nextProvider>
        );

        // the component is rendered async (the "Fetching cart data is displayed on first render") so we await the element to be ready
        // getByTestId() throws an error if the element will not be available.
        const emptyCartNode = await waitForElement(() => getByTestId('empty-minicart'));

        // compare the snapshow of the element with the stored one.
        expect(emptyCartNode).toMatchSnapshot();
    });
});
