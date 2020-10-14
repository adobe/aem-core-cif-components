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
import { MockedProvider } from '@apollo/client/testing';

import QUERY_CART_DETAILS from '../queries/query_cart_details.graphql';
import QUERY_COUNTRIES from '../queries/query_countries.graphql';
import QUERY_CUSTOMER_CART from '../queries/query_customer_cart.graphql';
import MUTATION_PLACE_ORDER from '../queries/mutation_place_order.graphql';
import MUTATION_SET_SHIPPING_ADDRESS from '../queries/mutation_set_shipping_address.graphql';

const emptyCartId = 'empty';
const mockCartId = '123ABC';
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
            query: QUERY_COUNTRIES,
            variables: {}
        },
        result: {
            data: {
                countries: []
            }
        }
    },
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
    },
    {
        request: {
            query: QUERY_CART_DETAILS,
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
            query: QUERY_CUSTOMER_CART
        },
        result: {
            data: {
                customerCart: {
                    id: 'customercart'
                }
            }
        }
    },
    {
        request: {
            query: MUTATION_PLACE_ORDER,
            variables: {
                cartId: ''
            }
        },
        result: {
            data: {
                order: {
                    order_id: 'orderid'
                }
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
