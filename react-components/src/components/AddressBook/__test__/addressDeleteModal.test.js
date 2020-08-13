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
import { MockedProvider } from '@apollo/react-testing';
import { I18nextProvider } from 'react-i18next';
import { render, fireEvent } from '@testing-library/react';

import UserContextProvider from '../../../context/UserContext';
import i18n from '../../../../__mocks__/i18nForTests';

import AddressDeleteModal from '../addressDeleteModal';

describe('<AddressDeleteModal>', () => {
    it('renders the component', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider>
                        <AddressDeleteModal />
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('handle click event of confirm delete address button', () => {
        const handler = jest.fn(state => state);

        const { getByText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider reducerFactory={() => handler}>
                        <AddressDeleteModal />
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        fireEvent.click(getByText('Delete'));

        expect(handler.mock.calls.length).toEqual(1);
    });

    it('handle click event of cancel delete address button', () => {
        const handler = jest.fn(state => state);

        const { getByText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider reducerFactory={() => handler}>
                        <AddressDeleteModal />
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        fireEvent.click(getByText('Cancel'));

        expect(handler.mock.calls.length).toEqual(1);
    });
});
