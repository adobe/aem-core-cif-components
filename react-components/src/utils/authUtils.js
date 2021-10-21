/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import { ApolloLink } from '@apollo/client';
import { checkCookie, cookieValue } from './cookieUtils';
import { checkItem, getJsonItem } from './localStorageUtils';

const SIGN_IN_TOKEN_KEY = 'M2_VENIA_BROWSER_PERSISTENCE__signin_token';

const authLink = new ApolloLink((operation, forward) => {
    let token = checkCookie('cif.userToken') ? cookieValue('cif.userToken') : '';
    if (checkItem(SIGN_IN_TOKEN_KEY)) {
        const singInTokenJson = getJsonItem(SIGN_IN_TOKEN_KEY);

        if (singInTokenJson) {
            const timestamp = new Date().getTime();
            if (timestamp - singInTokenJson.timeStored < singInTokenJson.ttl * 1000) {
                token = singInTokenJson.value.replaceAll('"', '');
            }
        }
    }

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
