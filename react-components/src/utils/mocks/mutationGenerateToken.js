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
import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';

export default {
    request: {
        query: MUTATION_GENERATE_TOKEN,
        variables: {
            email: 'chuck@example.com',
            password: 'norris'
        }
    },
    result: {
        data: {
            generateCustomerToken: {
                token: 'token-123'
            }
        }
    }
};
