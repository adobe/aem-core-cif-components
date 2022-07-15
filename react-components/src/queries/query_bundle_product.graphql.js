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
    query bundleProduct($sku: String!) {
        products(filter: { sku: { eq: $sku } }) {
            items {
                sku
                __typename
                id
                uid
                name
                ... on BundleProduct {
                    dynamic_sku
                    dynamic_price
                    dynamic_weight
                    price_view
                    ship_bundle_items
                    items {
                        option_id
                        title
                        required
                        type
                        position
                        sku
                        options {
                            id
                            quantity
                            position
                            is_default
                            price
                            price_type
                            can_change_quantity
                            label
                            product {
                                uid
                                price_range {
                                    maximum_price {
                                        final_price {
                                            currency
                                            value
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
`;
