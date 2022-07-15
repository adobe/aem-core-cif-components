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
import { gql } from '@apollo/client';

export default gql`
    query giftCardProduct($sku: String!) {
        products(filter: { sku: { eq: $sku } }) {
            items {
                id
                uid
                sku
                __typename
                name
                price_range {
                    maximum_price {
                        final_price {
                            currency
                        }
                    }
                }
                ... on GiftCardProduct {
                    giftcard_type
                    allow_open_amount
                    open_amount_min
                    open_amount_max
                    giftcard_amounts {
                        uid
                        value
                    }
                    gift_card_options {
                        title
                        required
                        ... on CustomizableFieldOption {
                            value {
                                uid
                            }
                        }
                    }
                }
            }
        }
    }
`;
