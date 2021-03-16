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
# Welcome to Altair GraphQL Client.
# You can send your request using CmdOrCtrl + Enter.

# Enter your graphQL query here.

mutation(
    $cartId: String!
    $city: String!
    $company: String
    $country_code: String!
    $firstname: String!
    $lastname: String!
    $postcode: String
    $region_code: String
    $save_in_address_book: Boolean
    $street: [String]!
    $telephone: String!
) {
    setBillingAddressOnCart(
        input: {
            cart_id: $cartId
            billing_address: {
                address: {
                    city: $city
                    company: $company
                    country_code: $country_code
                    firstname: $firstname
                    lastname: $lastname
                    postcode: $postcode
                    region: $region_code
                    save_in_address_book: $save_in_address_book
                    street: $street
                    telephone: $telephone
                }
            }
        }
    ) {
        cart {
            billing_address {
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
            }
        }
    }
}
`;
