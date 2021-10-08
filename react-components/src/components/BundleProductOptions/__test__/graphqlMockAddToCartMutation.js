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

import MUTATION_ADD_BUNDLE_TO_CART from '../../../queries/mutation_add_bundle_to_cart.graphql';
import mockResponse from './graphQlMockReponse';

export default {
    request: {
        query: MUTATION_ADD_BUNDLE_TO_CART,
        variables: {
            cartId: null,
            cartItems: [
                {
                    data: { sku: 'VA24', quantity: 1 },
                    bundle_options: [
                        { id: 1, quantity: 1, value: ['1'] },
                        { id: 2, quantity: 1, value: ['3'] },
                        { id: 3, quantity: 1, value: ['5'] },
                        { id: 4, quantity: 1, value: ['7', '8'] }
                    ]
                }
            ]
        }
    },
    result: mockResponse
};
