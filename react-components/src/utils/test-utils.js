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
import { render } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';

import QUERY_CART_DETAILS from '../queries/query_cart_details.graphql';

const emptyCartId = 'empty';
const mockCartId = '123ABC';

const mocks = [
    {
        request: {
            query: QUERY_CART_DETAILS,
            variables: {
                cartId: emptyCartId
            }
        },
        result: {
            data: {
                cart: {
                    email: null,
                    shipping_addresses: [],
                    prices: {
                        grand_total: {
                            currency: 'USD',
                            value: 0
                        }
                    },
                    selected_payment_method: {
                        code: '',
                        title: ''
                    },
                    billing_address: {
                        city: null,
                        country: {
                            code: null
                        },
                        lastname: null,
                        firstname: null,
                        region: {
                            code: null
                        },
                        street: [''],
                        postcode: null,
                        telephone: null
                    },
                    available_payment_methods: [
                        {
                            code: 'cashondelivery',
                            title: 'Cash On Delivery'
                        },
                        {
                            code: 'banktransfer',
                            title: 'Bank Transfer Payment'
                        },
                        {
                            code: 'checkmo',
                            title: 'Check / Money order'
                        },
                        {
                            code: 'free',
                            title: 'No Payment Information Required'
                        }
                    ],
                    items: []
                }
            }
        }
    },
    {
        request: {
            query: QUERY_CART_DETAILS,
            variables: {
                cartId: mockCartId
            }
        },
        result: {
            data: {
                prices: {
                    grand_total: {
                        currency: 'USD',
                        value: 0
                    }
                },
                items: []
            }
        }
    }
];

const AllProviders = ({ children }) => {
    return (
        <MockedProvider mocks={mocks} addTypename={false}>
            {children}
        </MockedProvider>
    );
};

/* Wrap all the React components tested with the library in a mocked Apollo provider */
const customRender = (ui, options) => render(ui, { wrapper: AllProviders, ...options });

export * from '@testing-library/react';
export { customRender as render };
