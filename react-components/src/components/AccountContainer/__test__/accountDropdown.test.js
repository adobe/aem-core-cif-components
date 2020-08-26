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
import { render } from '@testing-library/react';

import UserContextProvider from '../../../context/UserContext';
import { CartProvider } from '../../Minicart/cartContext';
import i18n from '../../../../__mocks__/i18nForTests';
import { ConfigContext } from '../../../context/ConfigContext';

import AccountDropdown from '../accountDropdown';

describe('<AccountDropdown>', () => {
    it('renders the component when account dropdown is open', () => {
        const stateWithAccountDropdownOpen = { isAccountDropdownOpen: true };
        const accountDropdownOpenClass = 'root_open';

        const { getByLabelText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={stateWithAccountDropdownOpen}>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <AccountDropdown />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(getByLabelText('account dropdown').getAttribute('class')).toEqual(accountDropdownOpenClass);
    });

    it('renders the my account component inside account dropdown', () => {
        const stateWithMyAccountView = {
            currentUser: {
                firstname: '',
                lastname: '',
                email: ''
            },
            isSignedIn: true,
            accountDropdownView: 'MY_ACCOUNT'
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <ConfigContext.Provider value={{}}>
                        <UserContextProvider initialState={stateWithMyAccountView}>
                            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                                <AccountDropdown />
                            </CartProvider>
                        </UserContextProvider>
                    </ConfigContext.Provider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the change password component inside account dropdown', () => {
        const stateWithChangePasswordView = {
            isSignedIn: true,
            accountDropdownView: 'CHANGE_PASSWORD'
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={stateWithChangePasswordView}>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <AccountDropdown />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the forgot password component inside account dropdown', () => {
        const stateWithForgotPasswordView = {
            isSignedIn: false,
            accountDropdownView: 'FORGOT_PASSWORD'
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={stateWithForgotPasswordView}>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <AccountDropdown />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the create account component inside account dropdown', () => {
        const stateWithCreateAccountView = {
            accountDropdownView: 'CREATE_ACCOUNT'
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={stateWithCreateAccountView}>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <AccountDropdown />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the account created component inside account dropdown', () => {
        const stateWithAccountCreatedView = {
            accountDropdownView: 'ACCOUNT_CREATED'
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={stateWithAccountCreatedView}>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <AccountDropdown />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });
});
