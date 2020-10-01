/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

const { deriveErrorMessage } = require('../deriveErrorMessage');

describe('deriveErrorMessage', () => {
    it('correctly builds an error message from an array of errors', () => {
        const errors = [
            {
                graphQLErrors: [{ message: 'Cannot query field X on type Y' }],
                message: 'This is an error'
            },
            {
                message: 'This is not a graphql error'
            }
        ];

        const fullMessage = deriveErrorMessage(errors);

        expect(fullMessage).toEqual('Cannot query field X on type Y, This is not a graphql error');
    });
});
