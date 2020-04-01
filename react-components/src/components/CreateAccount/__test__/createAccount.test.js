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
import { I18nextProvider } from 'react-i18next';

import UserContextProvider, { useUserContext } from '../../../context/UserContext';
import { CartProvider } from '../../Minicart/cartContext';
import CreateAccount from '../createAccount';
import i18n from '../../../../__mocks__/i18nForTests';

import MUTATION_GENERATE_TOKEN from '../../../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../../../queries/query_customer_details.graphql';
import MUTATION_CREATE_CUSTOMER from '../../../queries/mutation_create_customer.graphql';
import QUERY_CUSTOMER_CART from '../../../queries/query_customer_cart.graphql';
import MUTATION_MERGE_CARTS from '../../../queries/mutation_merge_carts.graphql';

describe('<CreateAccount>', () => {
    beforeEach(() => {
        jest.resetModules();
    });
    it('renders the component', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider>
                        <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                            <CreateAccount showMyAccount={jest.fn()} />
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('submits the form properly', async () => {
        const mockPerson = {
            email: 'imccoy@weretail.net',
            firstname: 'Iris',
            lastname: 'McCoy'
        };
        const mockPassword = 'Imccoy123';
        const mocks = [
            {
                request: {
                    query: MUTATION_CREATE_CUSTOMER,
                    variables: {
                        ...mockPerson,
                        password: mockPassword
                    }
                },
                result: {
                    data: {
                        createCustomer: {
                            customer: { ...mockPerson }
                        }
                    }
                }
            },
            {
                request: {
                    query: MUTATION_GENERATE_TOKEN,
                    variables: { email: mockPerson.email, password: mockPassword }
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
                        customer: mockPerson
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
            }
        ];

        const ContextWrapper = () => {
            const [{ isSignedIn, currentUser }] = useUserContext();
            let content;
            if (isSignedIn && currentUser.firstname) {
                content = <div data-testid="success">{currentUser.firstname}</div>;
            } else {
                content = <CreateAccount showMyAccount={jest.fn()} />;
            }

            return content;
        };

        const { getByLabelText, getByTestId, container } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{ cartId: 'guest123' }} reducerFactory={() => state => state}>
                        <ContextWrapper />
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );
        const detailsFromValue = value => {
            return {
                target: {
                    value
                }
            };
        };
        fireEvent.change(getByLabelText('firstname'), detailsFromValue(mockPerson.firstname));
        fireEvent.change(getByLabelText('lastname'), detailsFromValue(mockPerson.lastname));
        fireEvent.change(getByLabelText('email'), detailsFromValue(mockPerson.email));
        fireEvent.change(getByLabelText('password'), detailsFromValue(mockPassword));
        fireEvent.change(getByLabelText('confirm'), detailsFromValue(mockPassword));

        fireEvent.click(getByLabelText('submit'));

        const result = await waitForElement(() => {
            return getByTestId('success');
        });

        expect(container.querySelector('.root_error')).toBe(null);
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual(mockPerson.firstname);
    });

    it('handles the account creation error', async () => {
        const mockPerson = {
            email: 'imccoy@weretail.net',
            firstname: 'Iris',
            lastname: 'McCoy'
        };
        const mockPassword = 'Imccoy123';
        const mocks = [
            {
                request: {
                    query: MUTATION_CREATE_CUSTOMER,
                    variables: {
                        firstname: 'Iris',
                        lastname: 'McCoy',
                        email: 'imccoy@weretail.net',
                        password: 'Imccoy123'
                    }
                },
                result: {
                    errors: [
                        {
                            message: 'A customer with the same email address already exists in an associated website.',
                            category: 'graphql-input',
                            locations: [
                                {
                                    line: 2,
                                    column: 5
                                }
                            ],
                            path: ['createCustomer']
                        }
                    ],
                    data: {
                        createCustomer: null
                    }
                }
            }
        ];

        const ContextWrapper = () => {
            const [{ createAccountError }] = useUserContext();
            let content;
            if (createAccountError) {
                content = <div data-testid="success">{createAccountError}</div>;
            } else {
                content = <CreateAccount showMyAccount={jest.fn()} />;
            }

            return content;
        };

        const { getByTestId, getByLabelText } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{ cartId: 'guest123' }} reducerFactory={() => state => state}>
                        <ContextWrapper />
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        const detailsFromValue = value => {
            return {
                target: {
                    value
                }
            };
        };
        fireEvent.change(getByLabelText('firstname'), detailsFromValue(mockPerson.firstname));
        fireEvent.change(getByLabelText('lastname'), detailsFromValue(mockPerson.lastname));
        fireEvent.change(getByLabelText('email'), detailsFromValue(mockPerson.email));
        fireEvent.change(getByLabelText('password'), detailsFromValue(mockPassword));
        fireEvent.change(getByLabelText('confirm'), detailsFromValue(mockPassword));

        fireEvent.click(getByLabelText('submit'));

        const successMessage = await waitForElement(() => {
            return getByTestId('success');
        });

        expect(successMessage).not.toBeUndefined();
        expect(successMessage.textContent).toEqual(
            'A customer with the same email address already exists in an associated website.'
        );
    });
});
