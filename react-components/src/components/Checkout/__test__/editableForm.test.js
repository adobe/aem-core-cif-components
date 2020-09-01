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

import QUERY_COUNTRIES from '../../../queries/query_countries.graphql';
import CART_DETAILS_QUERY from '../../../queries/query_cart_details.graphql';

describe('<EditableForm />', () => {
    it('renders the shipping address form if countries are loaded', async () => {
        const mocks = [
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

        const { queryByText } = render(
            <I18nextProvider i18n={i18n}>
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
            </I18nextProvider>
        );

        const result = await waitForElement(() => {
            return queryByText('Shipping Address');
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

    it('should render the payments form if user is editing payments', async () => {
        const mocks = [
            {
                request: {
                    query: CART_DETAILS_QUERY
                },
                result: {
                    data: {
                        countries: [],
                        is_virtual: false,
                        shipping_addresses: [
                            {
                                city: 'asdf',
                                company: null,
                                country: { code: 'US', __typename: 'CartAddressCountry' },
                                firstname: 'Alex',
                                lastname: 'Kim',
                                postcode: '55057',
                                region: { code: 'CA', __typename: 'CartAddressRegion' },
                                street: ['1231'],
                                telephone: '123412341234',
                                available_shipping_methods: [
                                    {
                                        method_code: 'flatrate',
                                        method_title: 'Fixed',
                                        carrier_code: 'flatrate',
                                        carrier_title: 'Flat Rate',
                                        __typename: 'AvailableShippingMethod'
                                    }
                                ],
                                selected_shipping_method: null,
                                __typename: 'ShippingCartAddress'
                            }
                        ],
                        available_payment_methods: [
                            {
                                code: 'braintree_paypal',
                                title: 'PayPal (Braintree)',
                                __typename: 'AvailablePaymentMethod'
                            },
                            {
                                code: 'braintree',
                                title: 'Credit Card (Braintree)',
                                __typename: 'AvailablePaymentMethod'
                            },
                            { code: 'cashondelivery', title: 'Cash On Delivery', __typename: 'AvailablePaymentMethod' },
                            {
                                code: 'banktransfer',
                                title: 'Bank Transfer Payment',
                                __typename: 'AvailablePaymentMethod'
                            },
                            { code: 'checkmo', title: 'Check / Money order', __typename: 'AvailablePaymentMethod' }
                        ],
                        selected_payment_method: { code: '', title: '', __typename: 'SelectedPaymentMethod' },
                        billing_address: {
                            city: 'asdf',
                            country: { code: 'US', __typename: 'CartAddressCountry' },
                            lastname: 'Kim',
                            firstname: 'Alex',
                            region: { code: 'CA', __typename: 'CartAddressRegion' },
                            street: ['1231'],
                            postcode: '55057',
                            telephone: '123412341324',
                            __typename: 'BillingCartAddress'
                        },

                        __typename: 'Cart'
                    }
                }
            }
        ];

        const { queryByText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider mocks={mocks} addTypename={false}>
                    <UserContextProvider>
                        <CartProvider initialState={{}} reducerFactory={() => state => state}>
                            <CheckoutProvider
                                initialState={{ editing: 'paymentMethod', flowState: 'form' }}
                                reducer={state => state}>
                                <EditableForm />
                            </CheckoutProvider>
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        const result = await waitForElement(() => {
            return queryByText('Billing Information');
        });

        expect(result).not.toBeNull();
    });
});
