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

import React, { useState, useEffect } from 'react';
import { ApolloProvider } from '@apollo/react-hooks';
import ApolloClient from 'apollo-boost';
import { InMemoryCache } from 'apollo-cache-inmemory';
import { IntrospectionFragmentMatcher } from 'apollo-cache-inmemory';

import { CartProvider, CartInitializer } from '../Minicart';
import { CheckoutProvider } from '../Checkout';
import UserContextProvider from '../../context/UserContext';
import { checkCookie, cookieValue } from '../../utils/cookieUtils';
import { useConfigContext } from '../../context/ConfigContext';

const generateCacheData = introspectionQueryResultData => {
    const fragmentMatcher = new IntrospectionFragmentMatcher({
        introspectionQueryResultData
    });
    return new InMemoryCache({ fragmentMatcher });
};

const sessionStorage = window.sessionStorage;
let graphQlFragmentTypes = sessionStorage.getItem('graphQlFragmentTypes');

const App = props => {
    const { graphqlEndpoint, storeView = 'default', graphqlMethod = 'POST' } = useConfigContext();

    const clientConfig = {
        cache: graphQlFragmentTypes !== null ? generateCacheData(JSON.parse(graphQlFragmentTypes)) : undefined,
        uri: graphqlEndpoint,
        headers: { Store: storeView },
        request: operation => {
            let token = checkCookie('cif.userToken') ? cookieValue('cif.userToken') : '';
            if (token.length > 0) {
                operation.setContext({
                    headers: {
                        authorization: `Bearer ${token && token.length > 0 ? token : ''}`
                    }
                });
            }
        }
    };

    const [client, setClient] = useState(new ApolloClient(clientConfig));

    useEffect(() => {
        if (graphQlFragmentTypes === null) {
            fetch(graphqlEndpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    variables: {},
                    query: `
                        {
                            __schema {
                                types {
                                kind
                                name
                                possibleTypes {
                                    name
                                }
                                }
                            }
                        }
                    `
                })
            })
                .then(result => result.json())
                .then(result => {
                    // here we're filtering out any type information unrelated to unions or interfaces
                    const filteredData = result.data.__schema.types.filter(type => type.possibleTypes !== null);
                    result.data.__schema.types = filteredData;

                    graphQlFragmentTypes = JSON.stringify(result.data);
                    sessionStorage.setItem('graphQlFragmentTypes', graphQlFragmentTypes);

                    setClient(
                        new ApolloClient({
                            ...clientConfig,
                            cache: generateCacheData(result.data)
                        })
                    );
                });
        }
    });

    return (
        <ApolloProvider client={client}>
            <UserContextProvider>
                <CartProvider>
                    <CartInitializer>
                        <CheckoutProvider>{props.children}</CheckoutProvider>
                    </CartInitializer>
                </CartProvider>
            </UserContextProvider>
        </ApolloProvider>
    );
};

export default App;
