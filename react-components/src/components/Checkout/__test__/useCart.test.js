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
import { render, fireEvent } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';

import UserContextProvider from '../../../context/UserContext';
import { CartProvider } from '../../Minicart/cartContext';
import { CheckoutProvider, useCheckoutState } from '../checkoutContext';

import useCart from '../useCart';

import MUTATION_SET_SHIPPING_ADDRESS from '../../../queries/mutation_set_shipping_address.graphql';

describe('useCart', () => {
    it('begins checkout with billing address same as shipping address set to false', async () => {
        const MockCmp = () => {
            const { beginCheckout } = useCart();
            const { billingAddressSameAsShippingAddress } = useCheckoutState();

            return (
                <>
                    <div data-testid="same-as-shipping-address">
                        {!billingAddressSameAsShippingAddress ? 'false' : 'true'}
                    </div>
                    <button onClick={beginCheckout}>Begin Checkout</button>
                </>
            );
        };

        const mockCartState = {
            cart: {
                shipping_addresses: [
                    {
                        city: 'Calder',
                        country_code: 'US',
                        firstname: 'Veronica',
                        lastname: 'Costello',
                        postcode: '49628-7978',
                        region: {
                            code: 'MI'
                        },
                        street: ['cart shipping address'],
                        telephone: '(555) 229-3326'
                    }
                ],
                billing_address: {
                    city: 'Calder',
                    country_code: 'US',
                    firstname: 'Veronica',
                    lastname: 'Costello',
                    postcode: '49628-7978',
                    region: {
                        code: 'LA'
                    },
                    street: ['cart shipping address'],
                    telephone: '(555) 229-3326'
                }
            }
        };

        const checkoutHandler = jest.fn(state => state);

        const { getByTestId, getByRole } = render(
            <MockedProvider>
                <UserContextProvider>
                    <CartProvider initialState={mockCartState}>
                        <CheckoutProvider reducer={checkoutHandler}>
                            <MockCmp />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const saveAsShippingAddress = getByTestId('same-as-shipping-address');
        expect(saveAsShippingAddress.textContent).toEqual('false');
    });

    it('begins checkout with pre-filled shipping address', async () => {
        const MockCmp = () => {
            const { beginCheckout } = useCart();

            return <button onClick={beginCheckout}>Begin Checkout</button>;
        };

        const mockAddress = {
            city: 'Calder',
            country_code: 'US',
            company: '',
            firstname: 'Veronica',
            lastname: 'Costello',
            postcode: '49628-7978',
            region_code: 'MI',
            save_in_address_book: false,
            street: ['cart shipping address'],
            telephone: '(555) 229-3326'
        };

        const mocks = [
            {
                request: {
                    query: MUTATION_SET_SHIPPING_ADDRESS,
                    variables: {
                        cartId: '',
                        city: mockAddress.city,
                        country_code: mockAddress.country_code,
                        company: '',
                        firstname: mockAddress.firstname,
                        lastname: mockAddress.lastname,
                        postcode: mockAddress.postcode,
                        region_code: mockAddress.region_code,
                        save_in_address_book: false,
                        street: mockAddress.street,
                        telephone: mockAddress.telephone
                    }
                },
                result: {
                    data: {
                        cart: {
                            available_shipping_methods: {
                                carrier_code: '',
                                carrier_title: '',
                                method_code: '',
                                method_title: ''
                            },
                            city: mockAddress.city,
                            company: '',
                            country: {
                                code: mockAddress.country_code
                            },
                            firstname: mockAddress.firstname,
                            lastname: mockAddress.lastname,
                            postcode: mockAddress.postcode,
                            region: {
                                code: mockAddress.region_code
                            },
                            street: mockAddress.street,
                            telephone: mockAddress.telephone
                        }
                    }
                }
            }
        ];

        const mockUserState = {
            isSignedIn: true,
            currentUser: {
                addresses: [mockAddress]
            }
        };

        const mockCartState = {
            cartId: '',
            cart: {
                is_virtual: false
            }
        };

        const mockCheckoutState = {
            flowState: '',
            shippingAddress: null
        };

        const checkoutHandler = jest.fn(state => state);

        const { getByRole } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider initialState={mockUserState}>
                    <CartProvider initialState={mockCartState}>
                        <CheckoutProvider initialState={mockCheckoutState} reducer={checkoutHandler}>
                            <MockCmp />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));
    });
});
