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

import QUERY_CART_DETAILS from 'Queries/query_cart_details.graphql';
import QUERY_COUNTRIES from 'Queries/query_countries.graphql';

export const cart_details = {
    request: {
        query: QUERY_CART_DETAILS,
        variables: {
            cartId: 'cart-id'
        }
    },
    result: {
        data: {
            cart: {
                is_virtual: false,
                prices: {
                    discounts: null,
                    subtotal_with_discount_excluding_tax: { currency: 'USD', value: 78, __typename: 'Money' },
                    subtotal_excluding_tax: { currency: 'USD', value: 78, __typename: 'Money' },
                    grand_total: { currency: 'USD', value: 78, __typename: 'Money' },
                    __typename: 'CartPrices'
                },
                email: null,
                shipping_addresses: [],
                available_payment_methods: [
                    { code: 'checkmo', title: 'Check / Money order', __typename: 'AvailablePaymentMethod' }
                ],
                selected_payment_method: { code: '', title: '', __typename: 'SelectedPaymentMethod' },
                billing_address: null,
                applied_coupon: null,
                total_quantity: 1,
                items: [
                    {
                        id: '5',
                        quantity: 1,
                        prices: {
                            price: { currency: 'USD', value: 78, __typename: 'Money' },
                            row_total: { currency: 'USD', value: 78, __typename: 'Money' },
                            __typename: 'CartItemPrices'
                        },
                        product: {
                            name: 'Honora Wide Leg Pants',
                            sku: 'VP05-MT-S',
                            thumbnail: {},
                            __typename: 'SimpleProduct'
                        },
                        __typename: 'SimpleCartItem'
                    }
                ],
                __typename: 'Cart'
            }
        }
    }
};

export const cart_details_discount = {
    request: {
        query: QUERY_CART_DETAILS,
        variables: {
            cartId: 'cart-id'
        }
    },
    result: {
        data: {
            cart: {
                is_virtual: false,
                prices: {
                    discounts: [
                        {
                            amount: {
                                currency: 'USD',
                                value: 10,
                                __typename: 'Money'
                            },
                            label: 'Discount',
                            __typename: 'Discount'
                        }
                    ],
                    subtotal_with_discount_excluding_tax: {
                        currency: 'USD',
                        value: 68,
                        __typename: 'Money'
                    },
                    subtotal_excluding_tax: {
                        currency: 'USD',
                        value: 78,
                        __typename: 'Money'
                    },
                    grand_total: {
                        currency: 'USD',
                        value: 68,
                        __typename: 'Money'
                    },
                    __typename: 'CartPrices'
                },
                email: null,
                shipping_addresses: [],
                available_payment_methods: [
                    {
                        code: 'checkmo',
                        title: 'Check / Money order',
                        __typename: 'AvailablePaymentMethod'
                    }
                ],
                selected_payment_method: {
                    code: '',
                    title: '',
                    __typename: 'SelectedPaymentMethod'
                },
                billing_address: null,
                applied_coupon: {
                    code: 'OFF10',
                    __typename: 'AppliedCoupon'
                },
                total_quantity: 1,
                items: [
                    {
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
                            thumbnail: {},
                            __typename: 'SimpleProduct'
                        },
                        __typename: 'SimpleCartItem'
                    }
                ],
                __typename: 'Cart'
            }
        }
    }
};

export const countries = {
    request: {
        query: QUERY_COUNTRIES
    },
    result: {
        data: {
            countries: [
                {
                    id: 'US',
                    available_regions: [
                        {
                            id: 1,
                            name: 'Alabama',
                            code: 'AL',
                            __typename: 'Region'
                        },
                        {
                            id: 2,
                            name: 'Alaska',
                            code: 'AK',
                            __typename: 'Region'
                        },
                        {
                            id: 3,
                            name: 'American Samoa',
                            code: 'AS',
                            __typename: 'Region'
                        },
                        {
                            id: 4,
                            name: 'Arizona',
                            code: 'AZ',
                            __typename: 'Region'
                        },
                        {
                            id: 5,
                            name: 'Arkansas',
                            code: 'AR',
                            __typename: 'Region'
                        },
                        {
                            id: 6,
                            name: 'Armed Forces Africa',
                            code: 'AE',
                            __typename: 'Region'
                        },
                        {
                            id: 7,
                            name: 'Armed Forces Americas',
                            code: 'AA',
                            __typename: 'Region'
                        },
                        {
                            id: 8,
                            name: 'Armed Forces Canada',
                            code: 'AE',
                            __typename: 'Region'
                        },
                        {
                            id: 9,
                            name: 'Armed Forces Europe',
                            code: 'AE',
                            __typename: 'Region'
                        },
                        {
                            id: 10,
                            name: 'Armed Forces Middle East',
                            code: 'AE',
                            __typename: 'Region'
                        },
                        {
                            id: 11,
                            name: 'Armed Forces Pacific',
                            code: 'AP',
                            __typename: 'Region'
                        },
                        {
                            id: 12,
                            name: 'California',
                            code: 'CA',
                            __typename: 'Region'
                        },
                        {
                            id: 13,
                            name: 'Colorado',
                            code: 'CO',
                            __typename: 'Region'
                        },
                        {
                            id: 14,
                            name: 'Connecticut',
                            code: 'CT',
                            __typename: 'Region'
                        },
                        {
                            id: 15,
                            name: 'Delaware',
                            code: 'DE',
                            __typename: 'Region'
                        },
                        {
                            id: 16,
                            name: 'District of Columbia',
                            code: 'DC',
                            __typename: 'Region'
                        },
                        {
                            id: 17,
                            name: 'Federated States Of Micronesia',
                            code: 'FM',
                            __typename: 'Region'
                        },
                        {
                            id: 18,
                            name: 'Florida',
                            code: 'FL',
                            __typename: 'Region'
                        },
                        {
                            id: 19,
                            name: 'Georgia',
                            code: 'GA',
                            __typename: 'Region'
                        },
                        {
                            id: 20,
                            name: 'Guam',
                            code: 'GU',
                            __typename: 'Region'
                        },
                        {
                            id: 21,
                            name: 'Hawaii',
                            code: 'HI',
                            __typename: 'Region'
                        },
                        {
                            id: 22,
                            name: 'Idaho',
                            code: 'ID',
                            __typename: 'Region'
                        },
                        {
                            id: 23,
                            name: 'Illinois',
                            code: 'IL',
                            __typename: 'Region'
                        },
                        {
                            id: 24,
                            name: 'Indiana',
                            code: 'IN',
                            __typename: 'Region'
                        },
                        {
                            id: 25,
                            name: 'Iowa',
                            code: 'IA',
                            __typename: 'Region'
                        },
                        {
                            id: 26,
                            name: 'Kansas',
                            code: 'KS',
                            __typename: 'Region'
                        },
                        {
                            id: 27,
                            name: 'Kentucky',
                            code: 'KY',
                            __typename: 'Region'
                        },
                        {
                            id: 28,
                            name: 'Louisiana',
                            code: 'LA',
                            __typename: 'Region'
                        },
                        {
                            id: 29,
                            name: 'Maine',
                            code: 'ME',
                            __typename: 'Region'
                        },
                        {
                            id: 30,
                            name: 'Marshall Islands',
                            code: 'MH',
                            __typename: 'Region'
                        },
                        {
                            id: 31,
                            name: 'Maryland',
                            code: 'MD',
                            __typename: 'Region'
                        },
                        {
                            id: 32,
                            name: 'Massachusetts',
                            code: 'MA',
                            __typename: 'Region'
                        },
                        {
                            id: 33,
                            name: 'Michigan',
                            code: 'MI',
                            __typename: 'Region'
                        },
                        {
                            id: 34,
                            name: 'Minnesota',
                            code: 'MN',
                            __typename: 'Region'
                        },
                        {
                            id: 35,
                            name: 'Mississippi',
                            code: 'MS',
                            __typename: 'Region'
                        },
                        {
                            id: 36,
                            name: 'Missouri',
                            code: 'MO',
                            __typename: 'Region'
                        },
                        {
                            id: 37,
                            name: 'Montana',
                            code: 'MT',
                            __typename: 'Region'
                        },
                        {
                            id: 38,
                            name: 'Nebraska',
                            code: 'NE',
                            __typename: 'Region'
                        },
                        {
                            id: 39,
                            name: 'Nevada',
                            code: 'NV',
                            __typename: 'Region'
                        },
                        {
                            id: 40,
                            name: 'New Hampshire',
                            code: 'NH',
                            __typename: 'Region'
                        },
                        {
                            id: 41,
                            name: 'New Jersey',
                            code: 'NJ',
                            __typename: 'Region'
                        },
                        {
                            id: 42,
                            name: 'New Mexico',
                            code: 'NM',
                            __typename: 'Region'
                        },
                        {
                            id: 43,
                            name: 'New York',
                            code: 'NY',
                            __typename: 'Region'
                        },
                        {
                            id: 44,
                            name: 'North Carolina',
                            code: 'NC',
                            __typename: 'Region'
                        },
                        {
                            id: 45,
                            name: 'North Dakota',
                            code: 'ND',
                            __typename: 'Region'
                        },
                        {
                            id: 46,
                            name: 'Northern Mariana Islands',
                            code: 'MP',
                            __typename: 'Region'
                        },
                        {
                            id: 47,
                            name: 'Ohio',
                            code: 'OH',
                            __typename: 'Region'
                        },
                        {
                            id: 48,
                            name: 'Oklahoma',
                            code: 'OK',
                            __typename: 'Region'
                        },
                        {
                            id: 49,
                            name: 'Oregon',
                            code: 'OR',
                            __typename: 'Region'
                        },
                        {
                            id: 50,
                            name: 'Palau',
                            code: 'PW',
                            __typename: 'Region'
                        },
                        {
                            id: 51,
                            name: 'Pennsylvania',
                            code: 'PA',
                            __typename: 'Region'
                        },
                        {
                            id: 52,
                            name: 'Puerto Rico',
                            code: 'PR',
                            __typename: 'Region'
                        },
                        {
                            id: 53,
                            name: 'Rhode Island',
                            code: 'RI',
                            __typename: 'Region'
                        },
                        {
                            id: 54,
                            name: 'South Carolina',
                            code: 'SC',
                            __typename: 'Region'
                        },
                        {
                            id: 55,
                            name: 'South Dakota',
                            code: 'SD',
                            __typename: 'Region'
                        },
                        {
                            id: 56,
                            name: 'Tennessee',
                            code: 'TN',
                            __typename: 'Region'
                        },
                        {
                            id: 57,
                            name: 'Texas',
                            code: 'TX',
                            __typename: 'Region'
                        },
                        {
                            id: 58,
                            name: 'Utah',
                            code: 'UT',
                            __typename: 'Region'
                        },
                        {
                            id: 59,
                            name: 'Vermont',
                            code: 'VT',
                            __typename: 'Region'
                        },
                        {
                            id: 60,
                            name: 'Virgin Islands',
                            code: 'VI',
                            __typename: 'Region'
                        },
                        {
                            id: 61,
                            name: 'Virginia',
                            code: 'VA',
                            __typename: 'Region'
                        },
                        {
                            id: 62,
                            name: 'Washington',
                            code: 'WA',
                            __typename: 'Region'
                        },
                        {
                            id: 63,
                            name: 'West Virginia',
                            code: 'WV',
                            __typename: 'Region'
                        },
                        {
                            id: 64,
                            name: 'Wisconsin',
                            code: 'WI',
                            __typename: 'Region'
                        },
                        {
                            id: 65,
                            name: 'Wyoming',
                            code: 'WY',
                            __typename: 'Region'
                        }
                    ],
                    __typename: 'Country'
                }
            ]
        }
    }
};
