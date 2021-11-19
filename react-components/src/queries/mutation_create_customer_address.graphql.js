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
    mutation(
        $city: String
        $company: String
        $country_code: CountryCodeEnum
        $default_billing: Boolean
        $default_shipping: Boolean
        $firstname: String
        $lastname: String
        $postcode: String
        $region: CustomerAddressRegionInput
        $street: [String]
        $telephone: String
    ) {
        createCustomerAddress(
            input: {
                city: $city
                company: $company
                country_code: $country_code
                default_billing: $default_billing
                default_shipping: $default_shipping
                firstname: $firstname
                lastname: $lastname
                postcode: $postcode
                region: $region
                street: $street
                telephone: $telephone
            }
        ) {
            id
            city
            company
            country_code
            default_billing
            default_shipping
            firstname
            lastname
            postcode
            region {
                region_code
            }
            street
            telephone
        }
    }
`;
