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
import MUTATION_ADD_GIFT_CARD_TO_CART from '../../../queries/mutation_add_to_cart.graphql';

export default {
    request: {
        query: MUTATION_ADD_GIFT_CARD_TO_CART,
        variables: {
            cartId: 'Pi862RU1l0Zf4SoaehCKz6GAB5KmAEW3',
            cartItems: [
                {
                    sku: 'gift-card',
                    quantity: 1,
                    entered_options: [
                        {
                            uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=',
                            value: 'bla'
                        },
                        {
                            uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls',
                            value: 'bla'
                        },
                        {
                            uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=',
                            value: 'bla'
                        },
                        {
                            uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls',
                            value: 'bla'
                        },
                        {
                            uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==',
                            value: 'bla'
                        }
                    ],
                    selected_options: ['Z2lmdGNhcmQvZ2lmdGNhcmRfYW1vdW50LzEyLjAwMDA=']
                }
            ]
        }
    },
    result: {
        addProductsToCart: {
            cart: {
                id: 'test-id',
                items: [
                    {
                        uid: 'NjA=',
                        quantity: 1,
                        product: {
                            name: 'Gift Card',
                            thumbnail: {
                                url:
                                    'http://localhost:8080/static/version1636544588/frontend/Magento/luma/en_US/Magento_Catalog/images/product/placeholder/thumbnail.jpg',
                                __typename: 'ProductImage'
                            },
                            __typename: 'GiftCardProduct',
                            id: 2047,
                            url_key: 'gift-card',
                            url_suffix: '.html',
                            stock_status: 'IN_STOCK'
                        },
                        __typename: 'GiftCardCartItem',
                        id: '60',
                        prices: {
                            price: {
                                currency: 'USD',
                                value: 12,
                                __typename: 'Money'
                            },
                            __typename: 'CartItemPrices'
                        }
                    }
                ],
                total_quantity: 1,
                prices: {
                    subtotal_excluding_tax: {
                        currency: 'USD',
                        value: 12,
                        __typename: 'Money'
                    },
                    __typename: 'CartPrices'
                },
                __typename: 'Cart'
            },
            __typename: 'AddProductsToCartOutput'
        }
    }
};
