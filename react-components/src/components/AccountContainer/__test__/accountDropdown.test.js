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
import { render } from 'test-utils';
import { CartProvider } from '../../Minicart';
import AccountDropdown from '../accountDropdown';

const config = {
    graphqlEndpoint: 'endpoint',
    storeView: 'default',
    pagePaths: {
        accountDetails: '/accountDetails'
    }
};

describe('<AccountDropdown>', () => {
    it('renders the component when account dropdown is open', () => {
        const stateWithAccountDropdownOpen = { isAccountDropdownOpen: true };
        const accountDropdownOpenClass = 'root_open';

        const { getByLabelText } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <AccountDropdown />
            </CartProvider>,
            { config: config, userContext: stateWithAccountDropdownOpen }
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
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <AccountDropdown />
            </CartProvider>,
            { config: config, userContext: stateWithMyAccountView }
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the change password component inside account dropdown', () => {
        const stateWithChangePasswordView = {
            isSignedIn: true,
            accountDropdownView: 'CHANGE_PASSWORD'
        };

        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <AccountDropdown />
            </CartProvider>,
            { config: config, userContext: stateWithChangePasswordView }
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the forgot password component inside account dropdown', () => {
        const stateWithForgotPasswordView = {
            isSignedIn: false,
            accountDropdownView: 'FORGOT_PASSWORD'
        };

        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <AccountDropdown />
            </CartProvider>,
            { config: config, userContext: stateWithForgotPasswordView }
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the create account component inside account dropdown', () => {
        const stateWithCreateAccountView = {
            accountDropdownView: 'CREATE_ACCOUNT'
        };

        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <AccountDropdown />
            </CartProvider>,
            { config: config, userContext: stateWithCreateAccountView }
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the account created component inside account dropdown', () => {
        const stateWithAccountCreatedView = {
            accountDropdownView: 'ACCOUNT_CREATED'
        };

        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <AccountDropdown />
            </CartProvider>,
            { config: config, userContext: stateWithAccountCreatedView }
        );
        expect(asFragment()).toMatchSnapshot();
    });
});
