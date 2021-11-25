/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
import React from 'react';
import { wait, fireEvent } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import EditableForm from '../editableForm';
import { CartProvider } from '../../Minicart';
import { CheckoutProvider } from '../checkoutContext';
import mocksQueryCountries from '../../../utils/mocks/queryCountries';
import mocksCartDetails from '../../../utils/mocks/queryCart';
import mockAddress from '../../../utils/mocks/mockShippingAddress';

import CREATE_BRAINTREE_CLIENT_TOKEN from '../../../queries/mutation_create_braintree_client_token.graphql';
import MUTATION_SET_BILLING_ADDRESS from '../../../queries/mutation_set_billing_address.graphql';
import MUTATION_SET_PAYMENT_METHOD from '../../../queries/mutation_set_payment_method.graphql';
import MUTATION_SET_SHIPPING_ADDRESS from '../../../queries/mutation_set_shipping_address.graphql';
import MUTATION_SET_SHIPPING_METHOD from '../../../queries/mutation_set_shipping_method.graphql';
import QUERY_COUNTRIES from '../../../queries/query_countries.graphql';

describe('<EditableForm />', () => {
    /* eslint-disable no-unused-vars */
    const checkoutReducer = jest.fn((state, action) => state);
    const cartReducer = jest.fn((state, action) => state);
    const cartReducerFactory = jest.fn(setCartCookie => cartReducer);
    /* eslint-enable no-unused-vars */

    afterEach(() => {
        checkoutReducer.mockClear();
        cartReducer.mockClear();
        cartReducerFactory.mockClear();
    });

    it('renders the shipping address form if countries are loaded', async () => {
        const { queryByText, getByRole } = render(
            <CartProvider initialState={{}} reducerFactory={cartReducer}>
                <CheckoutProvider initialState={{ editing: 'address', flowState: 'form' }} reducer={checkoutReducer}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>
        );

        await wait(() => {
            expect(queryByText('Shipping Address')).not.toBeNull();
        });

        fireEvent.click(getByRole('button', { name: 'Cancel' }));
        expect(checkoutReducer).toHaveBeenCalledTimes(1);
        expect(checkoutReducer).toHaveBeenCalledWith(
            {
                editing: 'address',
                flowState: 'form'
            },
            { type: 'endEditing' }
        );
    });

    it('submits the Address form', async () => {
        const mocksAddressForm = [
            mocksQueryCountries,
            {
                request: {
                    query: MUTATION_SET_SHIPPING_ADDRESS,
                    variables: {
                        cartId: '123ABC',
                        country_code: 'US',
                        firstname: 'Veronica',
                        lastname: 'Costello',
                        email: 'veronica@example.com',
                        city: 'Calder',
                        region_code: 'MI',
                        postcode: '49628-7978',
                        telephone: '(555) 229-3326',
                        street: ['cart shipping address']
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
                                        city: 'Calder',
                                        company: 'mock company',
                                        country: {
                                            code: 'US'
                                        },
                                        firstname: 'Veronica',
                                        lastname: 'Costello',
                                        postcode: '49628-7978',
                                        region: {
                                            code: 'MI'
                                        },
                                        street: 'cart shipping address',
                                        telephone: '(555) 229-3326'
                                    }
                                ]
                            }
                        }
                    }
                }
            }
        ];
        const { asFragment, queryByText, getByRole } = render(
            <CartProvider initialState={{ cartId: '123ABC' }} reducerFactory={cartReducerFactory}>
                <CheckoutProvider
                    initialState={{
                        editing: 'address',
                        flowState: 'form',
                        shippingAddress: {
                            ...mockAddress,
                            email: 'veronica@example.com'
                        },
                        billingAddressSameAsShippingAddress: true
                    }}
                    reducer={checkoutReducer}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocksAddressForm }
        );

        await wait(() => {
            expect(queryByText('Shipping Address')).not.toBeNull();
        });

        fireEvent.click(getByRole('button', { name: 'Use Address' }));

        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('renders the payments form if countries are loaded', async () => {
        const mocksPaymentsForm = [
            mocksQueryCountries,
            mocksCartDetails,
            {
                request: {
                    query: CREATE_BRAINTREE_CLIENT_TOKEN
                },
                result: {
                    data: {
                        createBraintreeClientToken: 'my-sample-token'
                    }
                }
            },
            {
                request: {
                    query: MUTATION_SET_BILLING_ADDRESS,
                    variables: {
                        cartId: '123ABC',
                        country_code: 'US',
                        city: 'Calder',
                        company: 'shipping address company',
                        firstname: 'Veronica',
                        lastname: 'Costello',
                        postcode: '49628-7978',
                        region_code: 'MI',
                        save_in_address_book: false,
                        street: ['cart shipping address'],
                        telephone: '(555) 229-3326'
                    }
                },
                result: {
                    data: {
                        setBillingAddressOnCart: {
                            cart: {
                                billing_address: {
                                    country: {
                                        code: 'US'
                                    },
                                    city: 'Calder',
                                    company: 'shipping address company',
                                    firstname: 'Veronica',
                                    lastname: 'Costello',
                                    postcode: '49628-7978',
                                    region: {
                                        code: 'MI'
                                    },
                                    save_in_address_book: false,
                                    street: ['cart shipping address'],
                                    telephone: '(555) 229-3326'
                                }
                            }
                        }
                    }
                }
            },
            {
                request: {
                    query: MUTATION_SET_PAYMENT_METHOD,
                    variables: { cartId: '123ABC', paymentMethodCode: 'checkmo' }
                },
                result: {
                    data: {
                        setPaymentMethodOnCart: {
                            cart: {
                                selected_payment_method: {
                                    code: 'checkmo',
                                    title: 'Check / Money order'
                                }
                            }
                        }
                    }
                }
            }
        ];

        const mockCartState = {
            cartId: '123ABC',
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

        const { asFragment, queryByText, getByRole } = render(
            <CartProvider initialState={mockCartState} reducerFactory={cartReducerFactory}>
                <CheckoutProvider
                    initialState={{
                        editing: 'paymentMethod',
                        flowState: 'form',
                        shippingAddress: mockAddress,
                        billingAddressSameAsShippingAddress: true
                    }}
                    reducer={checkoutReducer}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocksPaymentsForm }
        );

        await wait(() => {
            expect(queryByText('Billing Information')).not.toBeNull();
            expect(checkoutReducer.mock.calls.length).toBe(1);
        });

        fireEvent.change(getByRole('combobox'), { target: { value: 'checkmo' } });
        fireEvent.click(getByRole('button', { name: 'Use Payment Method' }));

        await wait(() => {
            expect(checkoutReducer).toHaveBeenLastCalledWith(
                {
                    editing: 'paymentMethod',
                    flowState: 'form',
                    shippingAddress: {
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
                    },
                    billingAddressSameAsShippingAddress: true
                },
                { type: 'setPaymentMethod', paymentMethod: { code: 'checkmo', title: 'Check / Money order' } }
            );
        });

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the shipping method form if countries are loaded', async () => {
        const mocks = [
            mocksQueryCountries,
            mocksCartDetails,
            {
                request: {
                    query: MUTATION_SET_SHIPPING_METHOD,
                    variables: { cartId: '123ABC', carrier_code: 'flatrate', carrier_title: 'Flat Rate' }
                },
                result: {
                    data: {
                        setShippingMethodsOnCart: {
                            cart: {
                                shipping_addresses: [
                                    {
                                        selected_shipping_method: {
                                            method_code: 'flatrate',
                                            method_title: 'Fixed',
                                            carrier_code: 'flatrate',
                                            carrier_title: 'Flat Rate'
                                        }
                                    }
                                ]
                            }
                        }
                    }
                }
            }
        ];
        const { asFragment, getByRole, queryByText } = render(
            <CartProvider initialState={{ cartId: '123ABC' }} reducerFactory={cartReducerFactory}>
                <CheckoutProvider
                    initialState={{
                        editing: 'shippingMethod',
                        flowState: 'form',
                        shippingAddress: {
                            available_shipping_methods: [{ carrier_code: 'flatrate', carrier_title: 'Flat Rate' }]
                        }
                    }}
                    reducer={checkoutReducer}>
                    <EditableForm />
                </CheckoutProvider>
            </CartProvider>,
            { mocks: mocks }
        );

        await wait(() => {
            expect(queryByText('Shipping Information')).not.toBeNull();
        });

        fireEvent.change(getByRole('combobox'), { target: { value: 'flatrate' } });
        fireEvent.click(getByRole('button', { name: 'Use Method' }));

        expect(asFragment()).toMatchSnapshot();
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
            <CartProvider initialState={{}} reducerFactory={cartReducerFactory}>
                <CheckoutProvider initialState={{ editing: 'address', flowState: 'form' }} reducer={checkoutReducer}>
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
