/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
export default {
    type: 'ORDER_CONFIRMATION_PAGE_VIEW',
    payload: {
        order_number: '000000267',
        cart_id: 'I3XbPvRJhujwf2C2MWhevLgifr9xyXpL',
        amount: {
            grand_total: {
                value: 84.53,
                currency: 'USD',
                __typename: 'Money'
            },
            discounts: null,
            __typename: 'CartPrices'
        },
        shipping: [
            {
                amount: {
                    value: 0,
                    currency: 'USD',
                    __typename: 'Money'
                },
                carrier_title: 'Free Shipping',
                method_title: 'Free',
                __typename: 'SelectedShippingMethod'
            }
        ],
        payment: {
            purchase_order_number: null,
            title: 'Credit Card',
            __typename: 'SelectedPaymentMethod'
        },
        products: [
            {
                uid: 'MzA3NQ==',
                product: {
                    uid: 'OQ==',
                    sku: 'VA19-GO-NA',
                    name: 'Gold Omni Bangle Set',
                    thumbnail: {
                        url:
                            'https://master-7rqtwti-mfwmkrjfqvbjk.us-4.magentosite.cloud/media/catalog/product/cache/609faca36a4bc16a754bc2f43c184970/v/a/va19-go_main.jpg',
                        __typename: 'ProductImage'
                    },
                    __typename: 'SimpleProduct'
                },
                prices: {
                    price: {
                        currency: 'USD',
                        value: 78,
                        __typename: 'Money'
                    },
                    row_total: {
                        value: 78,
                        __typename: 'Money'
                    },
                    total_item_discount: {
                        value: 0,
                        __typename: 'Money'
                    },
                    __typename: 'CartItemPrices'
                },
                quantity: 1,
                __typename: 'SimpleCartItem'
            }
        ]
    }
};
