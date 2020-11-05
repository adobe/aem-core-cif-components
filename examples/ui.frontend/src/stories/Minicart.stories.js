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

import { I18nextProvider } from 'react-i18next';
import { MockedProvider } from '@apollo/client/testing';
import {
    ConfigContextProvider,
    CartProvider,
    Cart,
    CheckoutProvider,
    UserContextProvider
} from '@adobe/aem-core-cif-react-components';

import i18n from './i18n';
import { generateGithubLink } from './utils';
import { cart_details, cart_details_discount, countries } from './Minicart.mocks';

import './Minicart.css';

export default {
    title: 'Commerce/Cart',
    component: Cart,
    parameters: {
        docs: {
            description: {
                component: `The component is a client-side React component which displays the contents of the shopping cart. <br /><br />
                A shopper can use the component to change the quantity or remove a product from the cart.
                The cart data is read from the <code>CartProvider</code> context, which also takes care of all the operations a user can perform on the items in the cart.<br /><br />
                ${generateGithubLink(
                    'https://github.com/adobe/aem-core-cif-components/tree/master/react-components/src/components/Minicart'
                )}`
            }
        }
    }
};

const Template = (args, context) => {
    return (
        <I18nextProvider i18n={i18n} defaultNS="common">
            <ConfigContextProvider
                config={{
                    storeView: context.parameters.cifConfig.storeView,
                    graphqlEndpoint: context.parameters.cifConfig.graphqlEndpoint,
                    graphqlMethod: context.parameters.cifConfig.graphqlMethod
                }}>
                <MockedProvider mocks={args.mocks}>
                    <UserContextProvider>
                        <CartProvider initialState={args.cartState}>
                            <CheckoutProvider initialState={args.checkoutState}>
                                <Cart />
                            </CheckoutProvider>
                        </CartProvider>
                    </UserContextProvider>
                </MockedProvider>
            </ConfigContextProvider>
        </I18nextProvider>
    );
};

export const WithItems = Template.bind({});
WithItems.args = {
    cartState: {
        isOpen: true,
        isRegistered: false,
        isEditing: false,
        isLoading: false,
        editItem: {},
        cartId: 'cart-id',
        cart: {},
        errorMessage: null,
        couponError: null
    },
    mocks: [cart_details, countries]
};

export const EditItem = Template.bind({});
EditItem.args = {
    cartState: {
        isOpen: true,
        isRegistered: false,
        isEditing: true,
        isLoading: false,
        editItem: {
            id: '5',
            quantity: 1,
            prices: {
                price: {
                    currency: 'USD',
                    value: 78,
                    __typename: 'Money'
                },
                row_total: {
                    currency: 'USD',
                    value: 78,
                    __typename: 'Money'
                },
                __typename: 'CartItemPrices'
            },
            product: {
                name: 'Honora Wide Leg Pants',
                sku: 'VP05-MT-S',
                thumbnail: {
                    url:
                        'https://adobe-starter.dummycachetest.com/media/catalog/product/cache/e0d2bda07ae3887a54bc453b0b7bd41b/v/p/vp05-mt_main_1.jpg',
                    __typename: 'ProductImage'
                },
                __typename: 'SimpleProduct'
            },
            __typename: 'SimpleCartItem'
        },
        cartId: 'cart-id',
        cart: {},
        errorMessage: null,
        couponError: null
    },
    mocks: [cart_details, countries]
};
EditItem.parameters = {
    docs: {
        description: {
            story: 'Example of editing state of the cart to change the quantity of an item.'
        }
    }
};

export const WithDiscount = Template.bind({});
WithDiscount.args = {
    cartState: {
        isOpen: true,
        isRegistered: false,
        isEditing: false,
        isLoading: false,
        editItem: {},
        cartId: 'cart-id',
        cart: null,
        errorMessage: null,
        couponError: null
    },
    mocks: [cart_details_discount, countries]
};
WithDiscount.parameters = {
    docs: {
        description: {
            story: 'Example of a cart with applied discounts.'
        }
    }
};

export const Empty = Template.bind({});
Empty.args = {
    cartState: {
        isOpen: true,
        isRegistered: false,
        isEditing: false,
        isLoading: false,
        editItem: {},
        cartId: null,
        cart: null,
        errorMessage: null,
        couponError: null
    },
    mocks: []
};
Empty.parameters = {
    docs: {
        description: {
            story: 'Example of an empty cart.'
        }
    }
};

export const CheckoutForm = Template.bind({});
CheckoutForm.args = {
    cartState: {
        isOpen: true,
        isRegistered: false,
        isEditing: false,
        isLoading: false,
        editItem: {},
        cartId: 'cart-id',
        cart: {},
        errorMessage: null,
        couponError: null
    },
    mocks: [cart_details, countries, countries],
    checkoutState: {
        flowState: 'form',
        order: null,
        editing: null,
        shippingAddress: {
            city: 'San Jose',
            company: null,
            country: {
                code: 'US',
                __typename: 'CartAddressCountry'
            },
            firstname: 'John',
            lastname: 'Adobe',
            postcode: '95110',
            region: {
                code: 'CA',
                __typename: 'CartAddressRegion'
            },
            street: ['345 Park Ave'],
            telephone: '1234',
            available_shipping_methods: [
                {
                    method_code: 'flatrate',
                    method_title: 'Fixed',
                    carrier_code: 'flatrate',
                    carrier_title: 'Flat Rate',
                    __typename: 'AvailableShippingMethod'
                }
            ],
            selected_shipping_method: {
                carrier_code: 'flatrate',
                carrier_title: 'Flat Rate',
                method_code: 'flatrate',
                method_title: 'Fixed',
                __typename: 'SelectedShippingMethod'
            },
            __typename: 'ShippingCartAddress'
        },
        billingAddress: {
            city: 'San Jose',
            country: {
                code: 'US',
                __typename: 'CartAddressCountry'
            },
            lastname: 'Adobe',
            firstname: 'John',
            region: {
                code: 'CA',
                __typename: 'CartAddressRegion'
            },
            street: ['345 Park Ave'],
            postcode: '95110',
            telephone: '1234',
            __typename: 'BillingCartAddress'
        },
        billingAddressSameAsShippingAddress: true,
        isEditingNewAddress: false,
        shippingMethod: {
            carrier_code: 'flatrate',
            carrier_title: 'Flat Rate',
            method_code: 'flatrate',
            method_title: 'Fixed',
            __typename: 'SelectedShippingMethod'
        },
        paymentMethod: {
            code: 'checkmo',
            title: 'Check / Money order',
            __typename: 'SelectedPaymentMethod'
        },
        braintreeToken: false
    }
};
CheckoutForm.parameters = {
    docs: {
        description: {
            story:
                'Example of proceeding to the checkout in the cart. A user can fill out shipping and billing addresses, shipping and payment methods.'
        }
    }
};

export const OrderConfirmation = Template.bind({});
OrderConfirmation.args = {
    cartState: {
        isOpen: true,
        isRegistered: false,
        isEditing: false,
        isLoading: false,
        editItem: {},
        cartId: null,
        cart: null,
        errorMessage: null,
        couponError: null
    },
    mocks: [],
    checkoutState: {
        flowState: 'receipt',
        order: { __typename: 'Order', order_id: '000000001' },
        editing: 'receipt',
        shippingAddress: {
            city: 'San Jose',
            company: null,
            country: {
                code: 'US',
                __typename: 'CartAddressCountry'
            },
            firstname: 'John',
            lastname: 'Adobe',
            postcode: '95110',
            region: {
                code: 'CA',
                __typename: 'CartAddressRegion'
            },
            street: ['345 Park Ave'],
            telephone: '1234',
            available_shipping_methods: [
                {
                    method_code: 'flatrate',
                    method_title: 'Fixed',
                    carrier_code: 'flatrate',
                    carrier_title: 'Flat Rate',
                    __typename: 'AvailableShippingMethod'
                }
            ],
            selected_shipping_method: {
                carrier_code: 'flatrate',
                carrier_title: 'Flat Rate',
                method_code: 'flatrate',
                method_title: 'Fixed',
                __typename: 'SelectedShippingMethod'
            },
            __typename: 'ShippingCartAddress'
        },
        billingAddress: {
            city: 'San Jose',
            country: {
                code: 'US',
                __typename: 'CartAddressCountry'
            },
            lastname: 'Adobe',
            firstname: 'John',
            region: {
                code: 'CA',
                __typename: 'CartAddressRegion'
            },
            street: ['345 Park Ave'],
            postcode: '95110',
            telephone: '1234',
            __typename: 'BillingCartAddress'
        },
        billingAddressSameAsShippingAddress: true,
        isEditingNewAddress: false,
        shippingMethod: {
            carrier_code: 'flatrate',
            carrier_title: 'Flat Rate',
            method_code: 'flatrate',
            method_title: 'Fixed',
            __typename: 'SelectedShippingMethod'
        },
        paymentMethod: {
            code: 'checkmo',
            title: 'Check / Money order',
            __typename: 'SelectedPaymentMethod'
        },
        braintreeToken: false
    }
};
OrderConfirmation.parameters = {
    docs: {
        description: {
            story: 'Example of the order confirmation that is shown after submitting the checkout.'
        }
    }
};
