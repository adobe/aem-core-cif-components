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

/* eslint-disable react/prop-types */

import React from 'react';
import { render } from '../../../utils/test-utils';
import Minicart from '../minicart';
import { waitForElement } from '@testing-library/dom';
import { CartProvider } from '../cartContext';
import { CheckoutProvider } from '../../Checkout';

import QUERY_CART_DETAILS from '../../../queries/query_cart_details.graphql';

// avoid console errors logged during testing
console.error = jest.fn();

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
                    total_quantity: 0,
                    applied_coupon: null,
                    is_virtual: false,
                    email: null,
                    shipping_addresses: [],
                    prices: {
                        discounts: null,
                        subtotal_with_discount_excluding_tax: null,
                        subtotal_excluding_tax: null,
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

describe('<Minicart>', () => {
    it('renders the empty cart', async () => {
        const { getByTestId } = render(
            <CartProvider initialState={{ cartId: 'empty' }} reducerFactory={() => state => state}>
                <CheckoutProvider initialState={{ flowState: 'minicart' }} reducer={state => state}>
                    <Minicart />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocks }
        );

        // the component is rendered async (the "Fetching cart data is displayed on first render") so we await the element to be ready
        // getByTestId() throws an error if the element will not be available.
        const emptyCartNode = await waitForElement(() => getByTestId('empty-minicart'));

        // compare the snapshot of the element with the stored one.
        expect(emptyCartNode).toMatchSnapshot();
    });
});
