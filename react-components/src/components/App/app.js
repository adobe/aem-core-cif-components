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
import React from 'react';
import { ApolloClient, ApolloProvider, from, HttpLink, InMemoryCache } from '@apollo/client';

import { CartProvider, CartInitializer } from '../Minicart';
import { CheckoutProvider } from '../Checkout';
import UserContextProvider from '../../context/UserContext';
import { useConfigContext } from '../../context/ConfigContext';
import { graphqlAuthLink } from '../../utils/authUtils';

const App = props => {
    const { graphqlEndpoint, storeView = 'default' } = useConfigContext();

    const clientConfig = {
        link: from([graphqlAuthLink, new HttpLink({ uri: graphqlEndpoint, headers: { Store: storeView } })]),
        cache: new InMemoryCache()
    };

    const client = new ApolloClient(clientConfig);

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
