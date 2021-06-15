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
    query cartDetails($cartId: String!) {
        cart(cart_id: $cartId) {
            id
            is_virtual
            prices {
                discounts {
                    amount {
                        currency
                        value
                    }
                    label
                }
                applied_taxes {
                    amount {
                        currency
                        value
                    }
                    label
                }
                subtotal_with_discount_excluding_tax {
                    currency
                    value
                }
                subtotal_excluding_tax {
                    currency
                    value
                }
                subtotal_including_tax {
                    currency
                    value
                }
                grand_total {
                    currency
                    value
                }
            }
            email
            shipping_addresses {
                city
                company
                country {
                    code
                }
                firstname
                lastname
                postcode
                region {
                    code
                }
                street
                telephone
                available_shipping_methods {
                    method_code
                    method_title
                    carrier_code
                    carrier_title
                }
                selected_shipping_method {
                    carrier_code
                    carrier_title
                    method_code
                    method_title
                }
            }
            available_payment_methods {
                code
                title
            }
            selected_payment_method {
                code
                title
            }
            billing_address {
                city
                country {
                    code
                }
                lastname
                firstname
                region {
                    code
                }
                street
                postcode
                telephone
            }
            applied_coupon {
                code
            }
            total_quantity
            items {
                __typename
                uid
                quantity
                prices {
                    price {
                        currency
                        value
                    }
                    row_total {
                        currency
                        value
                    }
                }
                product {
                    name
                    sku
                    thumbnail {
                        url
                    }
                }
                ... on BundleCartItem {
                    bundle_options {
                        id
                        label
                        type
                        values {
                            id
                            label
                            price
                            quantity
                        }
                    }
                }
            }
        }
    }
`;
