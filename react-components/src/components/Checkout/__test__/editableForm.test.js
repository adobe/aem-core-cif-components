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
import { wait } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import EditableForm from '../editableForm';
import { CartProvider } from '../../Minicart';
import { CheckoutProvider } from '../checkoutContext';

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
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <CheckoutProvider initialState={{ editing: 'address', flowState: 'form' }} reducer={state => state}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocksQueryCountries }
        );

        await wait(() => {
            expect(queryByText('Shipping Address')).not.toBeNull();
        });
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

        let mockReducer = jest.fn(state => state);
        const { queryByText } = render(
            <CartProvider initialState={mockCartState} reducerFactory={() => state => state}>
                <CheckoutProvider initialState={{ editing: 'paymentMethod', flowState: 'form' }} reducer={mockReducer}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocksPaymentsForm }
        );

        await wait(() => {
            expect(queryByText('Billing Information')).not.toBeNull();
            expect(mockReducer.mock.calls.length).toBe(1);
        });
    });

    it('renders the shipping method form if countries are loaded', async () => {
        const { queryByText } = render(
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <CheckoutProvider
                    initialState={{ editing: 'shippingMethod', flowState: 'form' }}
                    reducer={state => state}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocksQueryCountries }
        );

        await wait(() => {
            expect(queryByText('Shipping Information')).not.toBeNull();
        });
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
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <CheckoutProvider initialState={{ editing: 'address', flowState: 'form' }} reducer={state => state}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocks }
        );

        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });
});
