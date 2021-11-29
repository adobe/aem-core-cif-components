/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
import GIFT_CARD_PRODUCT_QUERY from '../../../queries/query_gift_card_product.graphql';

export default {
    request: {
        query: GIFT_CARD_PRODUCT_QUERY,
        variables: {
            sku: 'gift-card'
        }
    },
    result: {
        data: {
            products: {
                items: [
                    {
                        sku: 'gift-card',
                        __typename: 'GiftCardProduct',
                        name: 'Gift Card',
                        price_range: {
                            maximum_price: {
                                final_price: {
                                    currency: 'USD',
                                    __typename: 'Money'
                                },
                                __typename: 'ProductPrice'
                            },
                            __typename: 'PriceRange'
                        },
                        giftcard_type: 'COMBINED',
                        allow_open_amount: true,
                        open_amount_min: 5,
                        open_amount_max: 23.5,
                        giftcard_amounts: [
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfYW1vdW50LzEyLjAwMDA=',
                                value: 12,
                                __typename: 'GiftCardAmounts'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfYW1vdW50LzEzLjAwMDA=',
                                value: 13,
                                __typename: 'GiftCardAmounts'
                            }
                        ],
                        gift_card_options: [
                            {
                                title: 'Sender Name',
                                required: true,
                                value: {
                                    uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=',
                                    __typename: 'CustomizableFieldValue'
                                },
                                __typename: 'CustomizableFieldOption'
                            },
                            {
                                title: 'Sender Email',
                                required: true,
                                value: {
                                    uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls',
                                    __typename: 'CustomizableFieldValue'
                                },
                                __typename: 'CustomizableFieldOption'
                            },
                            {
                                title: 'Recipient Name',
                                required: true,
                                value: {
                                    uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=',
                                    __typename: 'CustomizableFieldValue'
                                },
                                __typename: 'CustomizableFieldOption'
                            },
                            {
                                title: 'Recipient Email',
                                required: true,
                                value: {
                                    uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls',
                                    __typename: 'CustomizableFieldValue'
                                },
                                __typename: 'CustomizableFieldOption'
                            },
                            {
                                title: 'Message',
                                required: false,
                                value: {
                                    uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==',
                                    __typename: 'CustomizableFieldValue'
                                },
                                __typename: 'CustomizableFieldOption'
                            },
                            {
                                title: 'Custom Giftcard Amount',
                                required: false,
                                value: {
                                    uid: 'Z2lmdGNhcmQvY3VzdG9tX2dpZnRjYXJkX2Ftb3VudA==',
                                    __typename: 'CustomizableFieldValue'
                                },
                                __typename: 'CustomizableFieldOption'
                            }
                        ]
                    }
                ],
                __typename: 'Products'
            }
        }
    }
};
