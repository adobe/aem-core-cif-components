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

import UserContextProvider from '../../../context/UserContext';
import { CartProvider, useCartState } from '../../Minicart/cartContext';
import { CheckoutProvider, useCheckoutState } from '../checkoutContext';

import useCart from '../useCart';

import CART_DETAILS_QUERY from '../../../queries/query_cart_details.graphql';
import MUTATION_SET_SHIPPING_ADDRESS from '../../../queries/mutation_set_shipping_address.graphql';

describe('useCart', () => {
    const mockShippingAddress = {
        city: 'Calder',
        country_code: 'US',
        company: 'shipping address company',
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
                query: CART_DETAILS_QUERY,
                variables: {
                    cartId: ''
                }
            },
            result: {
                data: {
                    cart: {
                        shipping_addresses: [
                            {
                                available_shipping_methods: [
                                    {
                                        carrier_code: 'test carrier code',
                                        carrier_title: 'test carrier title',
                                        method_code: 'test method code',
                                        method_title: 'test method title'
                                    }
                                ],
                                city: mockShippingAddress.city,
                                company: mockShippingAddress.company,
                                country: {
                                    code: mockShippingAddress.country_code
                                },
                                firstname: mockShippingAddress.firstname,
                                lastname: mockShippingAddress.lastname,
                                postcode: mockShippingAddress.postcode,
                                region: {
                                    code: mockShippingAddress.region_code
                                },
                                street: mockShippingAddress.street,
                                telephone: mockShippingAddress.telephone
                            }
                        ]
                    }
                }
            }
        },
        {
            request: {
                query: MUTATION_SET_SHIPPING_ADDRESS,
                variables: {
                    cartId: null,
                    city: mockShippingAddress.city,
                    company: mockShippingAddress.company,
                    country_code: mockShippingAddress.country_code,
                    firstname: mockShippingAddress.firstname,
                    lastname: mockShippingAddress.lastname,
                    postcode: mockShippingAddress.postcode,
                    region_code: mockShippingAddress.region_code,
                    save_in_address_book: mockShippingAddress.save_in_address_book,
                    street: mockShippingAddress.street,
                    telephone: mockShippingAddress.telephone
                }
            },
            result: {
                data: {
                    setShippingAddressesOnCart: {
                        cart: {
                            shipping_addresses: [
                                {
                                    available_shipping_methods: [
                                        {
                                            carrier_code: 'test carrier code',
                                            carrier_title: 'test carrier title',
                                            method_code: 'test method code',
                                            method_title: 'test method title'
                                        }
                                    ],
                                    city: mockShippingAddress.city,
                                    company: mockShippingAddress.company,
                                    country: {
                                        code: mockShippingAddress.country_code
                                    },
                                    firstname: mockShippingAddress.firstname,
                                    lastname: mockShippingAddress.lastname,
                                    postcode: mockShippingAddress.postcode,
                                    region: {
                                        code: mockShippingAddress.region_code
                                    },
                                    street: mockShippingAddress.street,
                                    telephone: mockShippingAddress.telephone
                                }
                            ]
                        }
                    }
                }
            }
        }
    ];

    it('begins checkout when shipping address is set on cart', async () => {
        const Wrapper = () => {
            const { beginCheckout } = useCart();
            const [{ isLoading }] = useCartState();
            const [{ billingAddressSameAsShippingAddress, flowState }] = useCheckoutState();

            let content;
            if (!isLoading) {
                content = (
                    <>
                        <div data-testid="billing-address-same-as-shipping-address">
                            {!billingAddressSameAsShippingAddress ? 'false' : 'true'}
                        </div>
                        <div data-testid="flow-state">{flowState}</div>
                        <div data-testid="is-cart-loading">{isLoading ? 'true' : 'false'}</div>
                    </>
                );
            } else {
                content = <button onClick={beginCheckout}>Begin Checkout</button>;
            }

            return <div>{content}</div>;
        };

        const mockCartState = {
            cart: {
                shipping_addresses: [{ ...mockShippingAddress }],
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
            },
            isLoading: true
        };

        const mockCheckoutState = {
            flowState: '',
            billingAddressSameAsShippingAddress: true
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider>
                <UserContextProvider>
                    <CartProvider initialState={mockCartState}>
                        <CheckoutProvider initialState={mockCheckoutState}>
                            <Wrapper />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const billingAddressSaveAsShippingAddress = await waitForElement(() =>
            getByTestId('billing-address-same-as-shipping-address')
        );
        expect(billingAddressSaveAsShippingAddress).not.toBeUndefined();
        expect(billingAddressSaveAsShippingAddress.textContent).toEqual('false');

        const flowState = getByTestId('flow-state');
        expect(flowState).not.toBeUndefined();
        expect(flowState.textContent).toEqual('form');

        const isCartLoading = getByTestId('is-cart-loading');
        expect(isCartLoading).not.toBeUndefined();
        expect(isCartLoading.textContent).toEqual('false');
    });

    it('begins checkout when cart is virtual and only billing address is set on cart', async () => {
        const Wrapper = () => {
            const { beginCheckout } = useCart();
            const [{ isLoading }] = useCartState();
            const [{ billingAddressSameAsShippingAddress, flowState }] = useCheckoutState();

            let content;
            if (!isLoading) {
                content = (
                    <>
                        <div data-testid="billing-address-same-as-shipping-address">
                            {!billingAddressSameAsShippingAddress ? 'false' : 'true'}
                        </div>
                        <div data-testid="flow-state">{flowState}</div>
                        <div data-testid="is-cart-loading">{isLoading ? 'true' : 'false'}</div>
                    </>
                );
            } else {
                content = <button onClick={beginCheckout}>Begin Checkout</button>;
            }

            return <div>{content}</div>;
        };

        const mockCartState = {
            cart: {
                is_virtual: true,
                billing_address: {
                    ...mockShippingAddress,
                    region: {
                        code: 'LA'
                    }
                }
            },
            isLoading: true
        };

        const mockCheckoutState = {
            flowState: '',
            billingAddressSameAsShippingAddress: true
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider>
                <UserContextProvider>
                    <CartProvider initialState={mockCartState}>
                        <CheckoutProvider initialState={mockCheckoutState}>
                            <Wrapper />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const billingAddressSaveAsShippingAddress = await waitForElement(() =>
            getByTestId('billing-address-same-as-shipping-address')
        );
        expect(billingAddressSaveAsShippingAddress).not.toBeUndefined();
        expect(billingAddressSaveAsShippingAddress.textContent).toEqual('true');

        const flowState = getByTestId('flow-state');
        expect(flowState).not.toBeUndefined();
        expect(flowState.textContent).toEqual('form');

        const isCartLoading = getByTestId('is-cart-loading');
        expect(isCartLoading).not.toBeUndefined();
        expect(isCartLoading.textContent).toEqual('false');
    });

    it('begins checkout and calls set shipping address on cart with expected variables', async () => {
        const Wrapper = () => {
            const { beginCheckout } = useCart();
            const [{ isLoading }] = useCartState();
            const [{ flowState }] = useCheckoutState();

            let content;
            if (!isLoading) {
                content = (
                    <>
                        <div data-testid="flow-state">{flowState}</div>
                        <div data-testid="is-cart-loading">{isLoading ? 'true' : 'false'}</div>
                    </>
                );
            } else {
                content = <button onClick={beginCheckout}>Begin Checkout</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            isSignedIn: true,
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCartState = {
            cartId: null,
            cart: {
                is_virtual: false
            },
            isLoading: true
        };

        const mockCheckoutState = {
            flowState: '',
            shippingAddress: null
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider initialState={mockUserState}>
                    <CartProvider initialState={mockCartState}>
                        <CheckoutProvider initialState={mockCheckoutState}>
                            <Wrapper />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const flowState = await waitForElement(() => getByTestId('flow-state'));
        expect(flowState).not.toBeUndefined();
        expect(flowState.textContent).toEqual('form');

        const isCartLoading = getByTestId('is-cart-loading');
        expect(isCartLoading).not.toBeUndefined();
        expect(isCartLoading.textContent).toEqual('false');
    });

    it('begins checkout directly if the current user is a guest user', async () => {
        const Wrapper = () => {
            const { beginCheckout } = useCart();
            const [{ isLoading }] = useCartState();
            const [{ flowState }] = useCheckoutState();

            let content;
            if (!isLoading) {
                content = (
                    <>
                        <div data-testid="flow-state">{flowState}</div>
                        <div data-testid="is-cart-loading">{isLoading ? 'true' : 'false'}</div>
                    </>
                );
            } else {
                content = <button onClick={beginCheckout}>Begin Checkout</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            isSignedIn: false
        };

        const mockCartState = {
            isLoading: true
        };

        const mockCheckoutState = {
            flowState: ''
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider>
                <UserContextProvider initialState={mockUserState}>
                    <CartProvider initialState={mockCartState}>
                        <CheckoutProvider initialState={mockCheckoutState}>
                            <Wrapper />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const flowState = await waitForElement(() => getByTestId('flow-state'));
        expect(flowState).not.toBeUndefined();
        expect(flowState.textContent).toEqual('form');

        const isCartLoading = getByTestId('is-cart-loading');
        expect(isCartLoading).not.toBeUndefined();
        expect(isCartLoading.textContent).toEqual('false');
    });
});
