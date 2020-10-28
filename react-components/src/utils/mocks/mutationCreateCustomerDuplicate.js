/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import MUTATION_CREATE_CUSTOMER from '../../queries/mutation_create_customer.graphql';

export default {
    request: {
        query: MUTATION_CREATE_CUSTOMER,
        variables: {
            firstname: 'Iris',
            lastname: 'McCoy',
            email: 'imccoy@weretail.net',
            password: 'Imccoy123'
        }
    },
    result: {
        errors: [
            {
                message: 'A customer with the same email address already exists in an associated website.',
                category: 'graphql-input',
                locations: [
                    {
                        line: 2,
                        column: 5
                    }
                ],
                path: ['createCustomer']
            }
        ],
        data: {
            createCustomer: null
        }
    }
};
