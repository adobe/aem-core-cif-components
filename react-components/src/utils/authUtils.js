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
import { ApolloLink } from '@apollo/client';
import { checkCookie, cookieValue } from './cookieUtils';

const authLink = new ApolloLink((operation, forward) => {
    let token = checkCookie('cif.userToken') ? cookieValue('cif.userToken') : '';
    if (token.length > 0) {
        operation.setContext(({ headers }) => ({
            headers: {
                authorization: `Bearer ${token && token.length > 0 ? token : ''}`,
                ...headers
            }
        }));
    }

    return forward(operation);
});

export { authLink as graphqlAuthLink };
