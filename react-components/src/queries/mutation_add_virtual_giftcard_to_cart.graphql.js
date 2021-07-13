/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

import { gql } from '@apollo/client';

export default gql`
    mutation addProductsToCart($cartId: String!, $cartItems: [CartItemInput!]!) {
        addProductsToCart(cartId: $cartId, cartItems: $cartItems) {
            cart {
                items {
                    id
                    quantity
                    product {
                        sku
                    }
                    ... on GiftCardCartItem {
                        sender_name
                        sender_email
                        recipient_name
                        recipient_email
                        message
                        amount {
                            value
                            currency
                        }
                    }
                }
            }
        }
    }
`;
