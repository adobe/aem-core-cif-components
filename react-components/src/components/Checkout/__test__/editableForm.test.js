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
import { render, waitForElement } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import EditableForm from '../editableForm';
import { CartProvider } from '../../Minicart/cartContext';
import { CheckoutProvider } from '../checkoutContext';
import UserContextProvider from '../../../context/UserContext';
import i18n from '../../../../__mocks__/i18nForTests';

import CREATE_BRAINTREE_CLIENT_TOKEN from '../../../queries/mutation_create_braintree_client_token.graphql';
import QUERY_COUNTRIES from '../../../queries/query_countries.graphql';

describe('<EditableForm />', () => {
    const mocksQueryCountries = [
        {
            request: {
                query: QUERY_COUNTRIES
            },
            result: {
                data: {
                    countries: [
                        {
                            id: 'US',
                            available_regions: [
                                { id: 4, code: 'AL', name: 'Alabama' },
                                { id: 7, code: 'AK', name: 'Alaska' }
                            ]
                        }
                    ]
                }
            }
        }
    ];
    it('renders the shipping address form if countries are loaded', async () => {
        const { queryByText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider mocks={mocksQueryCountries} addTypename={false}>
                    <UserContextProvider>
                        <CartProvider initialState={{}} reducerFactory={() => state => state}>
                            <CheckoutProvider
                                initialState={{ editing: 'address', flowState: 'form' }}
                                reducer={state => state}>
                                <EditableForm />
                            </CheckoutProvider>
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        const result = await waitForElement(() => {
            return queryByText('Shipping Address');
        });

        expect(result).not.toBeNull();
    });

    it('renders the payments form if countries are loaded', async () => {
        const mocksPaymentsForm = [
            ...mocksQueryCountries,
            {
                request: {
                    query: CREATE_BRAINTREE_CLIENT_TOKEN
                },
                result: {
                    data: {
                        createBraintreeClientToken: 'my-sample-token'
                    }
                }
            }
        ];

        const mockCartState = {
            cart: {
                available_payment_methods: [
                    {
                        code: 'braintree',
                        title: 'Credit Card (Braintree)'
                    },
                    {
                        code: 'checkmo',
                        title: 'Check / Money order'
                    }
                ],
                is_virtual: false
            }
        };

        const { queryByText } = render(
            <MockedProvider mocks={mocksPaymentsForm} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={mockCartState} reducerFactory={() => state => state}>
                        <CheckoutProvider
                            initialState={{ editing: 'paymentMethod', flowState: 'form' }}
                            reducer={state => state}>
                            <EditableForm />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        const result = await waitForElement(() => {
            return queryByText('Billing Information');
        });

        expect(result).not.toBeNull();
    });

    it('renders the shipping method form if countries are loaded', async () => {
        const { queryByText } = render(
            <MockedProvider mocks={mocksQueryCountries} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{}} reducerFactory={() => state => state}>
                        <CheckoutProvider
                            initialState={{ editing: 'shippingMethod', flowState: 'form' }}
                            reducer={state => state}>
                            <EditableForm />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        const result = await waitForElement(() => {
            return queryByText('Shipping Information');
        });

        expect(result).not.toBeNull();
    });

    it('does not render the shipping address form if countries could not be loaded', async () => {
        const mocks = [
            {
                request: {
                    query: QUERY_COUNTRIES
                },
                result: {
                    data: {
                        countries: []
                    }
                }
            }
        ];

        const { asFragment } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{}} reducerFactory={() => state => state}>
                        <CheckoutProvider
                            initialState={{ editing: 'address', flowState: 'form' }}
                            reducer={state => state}>
                            <EditableForm />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
