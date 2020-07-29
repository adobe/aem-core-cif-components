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
import { render, fireEvent, waitForElement } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';

import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../../queries/query_customer_details.graphql';
import MUTATION_REVOKE_TOKEN from '../../queries/mutation_revoke_customer_token.graphql';
import MUTATION_CREATE_CART from '../../queries/mutation_create_guest_cart.graphql';
import QUERY_CUSTOMER_CART from '../../queries/query_customer_cart.graphql';

import UserContextProvider, { useUserContext } from '../UserContext';
import { useAwaitQuery } from '../../utils/hooks';

describe('UserContext test', () => {
    beforeEach(() => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: ''
        });
    });

    const mocks = [
        {
            request: {
                query: MUTATION_GENERATE_TOKEN,
                variables: {
                    email: 'imccoy@weretail.net',
                    password: 'imccoy123'
                }
            },
            result: {
                data: {
                    generateCustomerToken: {
                        token: 'token123'
                    }
                }
            }
        },
        {
            request: {
                query: QUERY_CUSTOMER_DETAILS
            },
            result: {
                data: {
                    customer: {
                        email: 'test@example.com',
                        firstname: 'John',
                        lastname: 'Doe'
                    }
                }
            }
        },
        {
            request: {
                query: MUTATION_REVOKE_TOKEN
            },
            result: {
                data: {
                    revokeCustomerToken: {
                        result: true
                    }
                }
            }
        },
        {
            request: {
                query: MUTATION_CREATE_CART
            },
            result: {
                data: {
                    createEmptyCart: {
                        id: 'guest123'
                    }
                }
            }
        },
        {
            request: {
                query: QUERY_CUSTOMER_CART
            },
            result: {
                data: {
                    customerCart: {
                        id: 'customercart'
                    }
                }
            }
        }
    ];

    it('updates the user token in state', async () => {
        const ContextWrapper = () => {
            const [{ token, isSignedIn }, { setToken }] = useUserContext();

            let content;
            if (isSignedIn) {
                content = <div data-testid="success">{token}</div>;
            } else {
                content = <button onClick={() => setToken('guest123')}>Sign in</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('guest123');
    });

    it('updates the cart id of the user', async () => {
        const ContextWrapper = () => {
            const [{ cartId }, { setCustomerCart }] = useUserContext();

            let content;
            if (cartId) {
                content = <div data-testid="success">{cartId}</div>;
            } else {
                content = <button onClick={() => setCustomerCart('guest123')}>Update cart id</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('guest123');
    });

    it('resets the customer cart', async () => {
        const ContextWrapper = () => {
            const [{ cartId }, { resetCustomerCart }] = useUserContext();
            const fetchCustomerCartQuery = useAwaitQuery(QUERY_CUSTOMER_CART);

            let content;
            if (cartId) {
                content = <div data-testid="success">{cartId}</div>;
            } else {
                content = <button onClick={() => resetCustomerCart(fetchCustomerCartQuery)}>Reset cart</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('customercart');
    });

    it('performs a sign out', async () => {
        const ContextWrapper = () => {
            const [{ isSignedIn }, { signOut }] = useUserContext();
            let content;
            if (isSignedIn) {
                content = (
                    <div>
                        <span>Signed in</span>
                        <button onClick={() => signOut()}>{'Sign out'}</button>
                    </div>
                );
            } else {
                content = <div data-testid="success">{'Signed out'}</div>;
            }

            return <div>{content}</div>;
        };

        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.userToken=token123;'
        });

        const { getByRole, getByTestId, getByText } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByText('Signed in')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));

        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();

        // normally the browser just removes a cookie with Max-Age=0
        //...but we're not in a browser
        const expectedCookieValue = 'cif.userToken=;path=/; domain=localhost;Max-Age=0';
        expect(document.cookie).toEqual(expectedCookieValue);
    });

    it('open account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ isAccountDropdownOpen }, { toggleAccountDropdown }] = useUserContext();

            let content;
            if (isAccountDropdownOpen) {
                content = <div data-testid="account-dropdown-open">Account dropdown opened</div>;
            } else {
                content = <button onClick={() => toggleAccountDropdown(true)}>Open account dropdown</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('account-dropdown-open'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Account dropdown opened');
    });

    it('show sign in view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showForgotPassword, showSignIn }] = useUserContext();

            let content;
            if (accountDropdownView === 'SIGN_IN') {
                content = (
                    <>
                        <div data-testid="sign-in-view">Sign-in view shown</div>
                        <button onClick={() => showForgotPassword()}>Show forgot password view</button>
                    </>
                );
            } else {
                content = (
                    <>
                        <div data-testid="forgot-password-view">Forgot password view shown</div>;
                        <button data-testid="show-sign-in-button" onClick={() => showSignIn()}>
                            Show sign-in view
                        </button>
                    </>
                );
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const forgotPasswordView = await waitForElement(() => getByTestId('forgot-password-view'));
        expect(forgotPasswordView).not.toBeUndefined();
        expect(forgotPasswordView.textContent).toEqual('Forgot password view shown');

        const showSignInButton = getByTestId('show-sign-in-button');
        expect(showSignInButton).not.toBeUndefined();
        fireEvent.click(showSignInButton);

        const result = await waitForElement(() => getByTestId('sign-in-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Sign-in view shown');
    });

    it('show create account view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showCreateAccount }] = useUserContext();

            let content;
            if (accountDropdownView === 'CREATE_ACCOUNT') {
                content = <div data-testid="create-account-view">Create account view shown</div>;
            } else {
                content = <button onClick={() => showCreateAccount()}>Show create account view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('create-account-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Create account view shown');
    });

    it('show my account view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showMyAccount }] = useUserContext();

            let content;
            if (accountDropdownView === 'MY_ACCOUNT') {
                content = <div data-testid="my-account-view">My account view shown</div>;
            } else {
                content = <button onClick={() => showMyAccount()}>Show my account view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('my-account-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('My account view shown');
    });

    it('show account created view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showAccountCreated }] = useUserContext();

            let content;
            if (accountDropdownView === 'ACCOUNT_CREATED') {
                content = <div data-testid="account-created-view">Account created view shown</div>;
            } else {
                content = <button onClick={() => showAccountCreated()}>Show account created view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('account-created-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Account created view shown');
    });

    it('show change password view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showChangePassword }] = useUserContext();

            let content;
            if (accountDropdownView === 'CHANGE_PASSWORD') {
                content = <div data-testid="change-password-view">Change password view shown</div>;
            } else {
                content = <button onClick={() => showChangePassword()}>Show Change password view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('change-password-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Change password view shown');
    });
});
