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
import { fireEvent, wait } from '@testing-library/react';
import { render } from 'test-utils';
import { act } from 'react-dom/test-utils';

import { TextEncoder } from 'util';
import { Crypto } from '@peculiar/webcrypto';

import { CartProvider } from '../../Minicart';
import { CheckoutProvider, useCheckoutState } from '../checkoutContext';
import useOverview from '../useOverview';
import mockMagentoStorefrontEvents from '../../../utils/mocks/mockMagentoStorefrontEvents';

describe('useOverview', () => {
    let mse;
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

    beforeAll(() => {
        window.TextEncoder = TextEncoder;
        window.crypto = new Crypto();

        window.document.body.setAttributeNode(document.createAttribute('data-cmp-data-layer-enabled'));
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;

        window.adobeDataLayer = [];
        window.adobeDataLayer.push = jest.fn();
    });

    beforeEach(() => {
        window.adobeDataLayer.push.mockClear();
        window.magentoStorefrontEvents.mockClear();
    });

    it('edits new shipping address', async () => {
        const Wrapper = () => {
            const [, { editShippingAddress }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress, firstname: 'firstname' };

            let content;
            if (isEditingNewAddress) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editShippingAddress(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        await wait(() => {
            const isEditingNewAddress = getByTestId('is-editing-new-address');
            expect(isEditingNewAddress).not.toBeUndefined();
            expect(isEditingNewAddress.textContent).toEqual('true');

            const editing = getByTestId('editing');
            expect(editing).not.toBeUndefined();
            expect(editing.textContent).toEqual('address');
        });
    });

    it('edits saved shipping address', async () => {
        const Wrapper = () => {
            const [, { editShippingAddress }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress };

            let content;
            if (editing) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editShippingAddress(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        await wait(() => {
            const isEditingNewAddress = getByTestId('is-editing-new-address');
            expect(isEditingNewAddress).not.toBeUndefined();
            expect(isEditingNewAddress.textContent).toEqual('false');

            const editing = getByTestId('editing');
            expect(editing).not.toBeUndefined();
            expect(editing.textContent).toEqual('address');
        });
    });

    it('edits billing information with the billing address that is the same as the shipping address', async () => {
        const Wrapper = () => {
            const [, { editBillingInformation }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress };

            let content;
            if (isEditingNewAddress) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editBillingInformation(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false,
            billingAddressSameAsShippingAddress: true
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        await wait(() => {
            const isEditingNewAddress = getByTestId('is-editing-new-address');
            expect(isEditingNewAddress).not.toBeUndefined();
            expect(isEditingNewAddress.textContent).toEqual('true');

            const editing = getByTestId('editing');
            expect(editing).not.toBeUndefined();
            expect(editing.textContent).toEqual('paymentMethod');
        });
    });

    it('edits billing information with saved billing address', async () => {
        const Wrapper = () => {
            const [, { editBillingInformation }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress };

            let content;
            if (editing) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editBillingInformation(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false,
            billingAddressSameAsShippingAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        await wait(() => {
            const isEditingNewAddress = getByTestId('is-editing-new-address');
            expect(isEditingNewAddress).not.toBeUndefined();
            expect(isEditingNewAddress.textContent).toEqual('false');

            const editing = getByTestId('editing');
            expect(editing).not.toBeUndefined();
            expect(editing.textContent).toEqual('paymentMethod');
        });
    });

    it('places order', async () => {
        const Wrapper = () => {
            const [, { placeOrder }] = useOverview();

            return <button onClick={() => placeOrder()}>Place order</button>;
        };

        const mockCartState = {
            isOpen: false,
            isRegistered: false,
            isEditing: false,
            isLoading: false,
            editItem: {},
            cartId: 'guest123',
            cart: {
                is_virtual: false,
                prices: {
                    discounts: null,
                    applied_taxes: [],
                    subtotal_with_discount_excluding_tax: {
                        currency: 'USD',
                        value: 22
                    },
                    subtotal_excluding_tax: {
                        currency: 'USD',
                        value: 22
                    },
                    subtotal_including_tax: {
                        currency: 'USD',
                        value: 22
                    },
                    grand_total: {
                        currency: 'USD',
                        value: 27
                    }
                },
                email: 'user@example.com',
                shipping_addresses: [
                    {
                        city: 'Bucuresti',
                        company: null,
                        country: {
                            code: 'RO'
                        },
                        firstname: 'Some',
                        lastname: 'User',
                        postcode: '012321',
                        region: {
                            code: 'B'
                        },
                        street: ['Some address'],
                        telephone: '000000000',
                        available_shipping_methods: [
                            {
                                method_code: 'flatrate',
                                method_title: 'Fixed',
                                carrier_code: 'flatrate',
                                carrier_title: 'Flat Rate'
                            }
                        ],
                        selected_shipping_method: {
                            amount: {
                                currency: 'USD',
                                value: 5
                            },
                            carrier_code: 'flatrate',
                            carrier_title: 'Flat Rate',
                            method_code: 'flatrate',
                            method_title: 'Fixed'
                        }
                    }
                ],
                available_payment_methods: [
                    {
                        code: 'checkmo',
                        title: 'Check / Money order'
                    }
                ],
                selected_payment_method: {
                    code: 'checkmo',
                    title: 'Check / Money order'
                },
                billing_address: {
                    city: 'Bucuresti',
                    country: {
                        code: 'RO'
                    },
                    lastname: 'Some',
                    firstname: 'User',
                    region: {
                        code: 'B'
                    },
                    street: ['Some address'],
                    postcode: '012321',
                    telephone: '000000000'
                },
                applied_coupon: {
                    code: 'somecoupon'
                },
                total_quantity: 1,
                items: [
                    {
                        __typename: 'SimpleCartItem',
                        quantity: 1,
                        prices: {
                            price: {
                                currency: 'USD',
                                value: 22
                            },
                            row_total: {
                                currency: 'USD',
                                value: 22
                            }
                        },
                        product: {
                            name: 'Radiant Tee-M-Orange',
                            sku: 'WS12-M-Orange',
                            thumbnail: {
                                url:
                                    'http://localhost:8080/media/catalog/product/cache/3431b869eac756ac84605eb6066d9c1e/w/s/ws12-orange_main_2.jpg'
                            }
                        }
                    }
                ]
            },
            errorMessage: null,
            couponError: null
        };

        const mockCheckoutState = {
            flowState: 'form',
            editing: null,
            isEditingNewAddress: false,
            billingAddressSameAsShippingAddress: false
        };

        const { getByRole } = render(
            <CartProvider initialState={mockCartState}>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();
        await act(async () => fireEvent.click(getByRole('button')));

        expect(mse.context.setOrder).toHaveBeenCalledTimes(1);
        expect(mse.context.setOrder).toHaveBeenCalledWith({
            appliedCouponCode: 'somecoupon',
            email: 'user@example.com',
            grandTotal: 27,
            orderId: '000000005',
            otherTax: 5,
            paymentMethodCode: 'checkmo',
            paymentMethodName: 'Check / Money order',
            salesTax: 0,
            subtotalExcludingTax: 22,
            subtotalIncludingTax: 22
        });
        expect(mse.publish.placeOrder).toHaveBeenCalledTimes(1);
    });
});
