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
import React, { useState } from 'react';
import { render, fireEvent, waitForElement } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';
import { I18nextProvider } from 'react-i18next';

import UserContextProvider from '../../../context/UserContext';
import { CartProvider } from '../../Minicart/cartContext';

import MUTATION_GENERATE_TOKEN from '../../../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../../../queries/query_customer_details.graphql';
import QUERY_CUSTOMER_CART from '../../../queries/query_customer_cart.graphql';
import MUTATION_MERGE_CARTS from '../../../queries/mutation_merge_carts.graphql';
import SignIn from '../signIn';
import i18n from '../../../../__mocks__/i18nForTests';

const mocks = [
    {
        request: {
            query: MUTATION_GENERATE_TOKEN,
            variables: {
                email: 'chuck@example.com',
                password: 'norris'
            }
        },
        result: {
            data: {
                generateCustomerToken: {
                    token: 'token-123'
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
            query: MUTATION_MERGE_CARTS,
            variables: {
                sourceCartId: 'guest123',
                destinationCartId: 'customercart'
            }
        },
        result: {
            data: {
                mergeCarts: {
                    id: 'customercart'
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

describe('<SignIn>', () => {
    beforeEach(() => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=guest123'
        });
    });

    it('renders the component', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <SignIn
                                showMyAccount={jest.fn()}
                                showCreateAccount={jest.fn()}
                                showForgotPassword={jest.fn()}
                            />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
    it('switch the view then the "Sign In" is successful', async () => {
        // To simulate an almost real use case of the sign in component we create a wrapper around it
        // which displays a "success" message when the user is signed in
        const SignInWrapper = () => {
            const [signedIn, setSignedIn] = useState(false);

            const showMyAccount = () => {
                setSignedIn(true);
            };
            let content;
            if (signedIn) {
                content = <div data-testid="success">Done</div>;
            } else {
                content = (
                    <SignIn
                        showMyAccount={showMyAccount}
                        showCreateAccount={jest.fn()}
                        showForgotPassword={jest.fn()}
                    />
                );
            }

            return <div>{content}</div>;
        };

        const { getByTestId, getByLabelText } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                        <SignInWrapper />
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.change(getByLabelText(/email/i), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText(/password/i), { target: { value: 'norris' } });
        fireEvent.click(getByLabelText('submit'));

        const result = await waitForElement(() => getByTestId('success'));

        expect(result.textContent).not.toBeUndefined();
    });

    it('shows an error when the sign in is not successful', async () => {
        const mocks = [
            {
                request: {
                    query: MUTATION_GENERATE_TOKEN,
                    variables: {
                        email: 'chuck@example.com',
                        password: 'wrongpassword'
                    }
                },
                result: {
                    data: {
                        generateCustomerToken: null
                    },
                    errors: [
                        {
                            message: 'Error',
                            category: 'graphql-authentication'
                        }
                    ]
                }
            }
        ];

        const { getByText, getByLabelText } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                        <SignIn
                            showMyAccount={jest.fn()}
                            showForgotPassword={jest.fn()}
                            showCreateAccount={jest.fn()}
                        />
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.change(getByLabelText(/email/i), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText(/password/i), { target: { value: 'wrongpassword' } });
        fireEvent.click(getByLabelText('submit'));

        const result = await waitForElement(() => getByText('Error'));

        expect(result).not.toBeUndefined();
    });
});
