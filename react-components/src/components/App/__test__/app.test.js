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

jest.mock('@apollo/client', () => ({
    ...jest.requireActual('@apollo/client'),
    ApolloClient: jest.fn(config => config),
    HttpLink: jest.fn(config => config),
    from: jest.fn(config => config)
}));

import React from 'react';
import { ApolloClient, useApolloClient } from '@apollo/client';
import '@testing-library/jest-dom';

import { render } from '../../../utils/test-utils';
import App from '../app';
import ConfigContext from '../../../context/ConfigContext';

describe('<App />', () => {
    const MockComponent = () => {
        const client = useApolloClient();

        return (
            <div>
                <div data-testid="has-client">{client ? 'true' : 'false'}</div>
            </div>
        );
    };

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('configures the ApolloProvider', () => {
        const config = {
            storeView: 'my-store',
            headers: {
                'X-Custom-Heade': 'foobar'
            },
            graphqlEndpoint: '/api/graphql',
            graphqlMethod: 'POST'
        };

        const { getByTestId } = render(
            <ConfigContext config={config}>
                <App>
                    <MockComponent />
                </App>
            </ConfigContext>
        );

        expect(getByTestId('has-client')).toHaveTextContent('true');
        expect(ApolloClient).toHaveBeenCalledTimes(1);

        const apolloConfig = ApolloClient.mock.calls[0][0];
        const apolloHttpLink = apolloConfig.link[1];

        expect(apolloHttpLink).toMatchObject({
            uri: '/api/graphql',
            headers: {
                'X-Custom-Heade': 'foobar',
                Store: 'my-store'
            },
            useGETForQueries: false
        });
    });

    it('omits the Store header when no storeView is set', () => {
        const config = {
            graphqlEndpoint: '/api/graphql',
            graphqlMethod: 'GET'
        };

        const { getByTestId } = render(
            <ConfigContext config={config}>
                <App>
                    <MockComponent />
                </App>
            </ConfigContext>
        );

        expect(getByTestId('has-client')).toHaveTextContent('true');
        expect(ApolloClient).toHaveBeenCalledTimes(1);

        const apolloConfig = ApolloClient.mock.calls[0][0];
        const apolloHttpLink = apolloConfig.link[1];

        expect(apolloHttpLink.headers).toEqual({});
        expect(apolloHttpLink).toMatchObject({
            uri: '/api/graphql',
            useGETForQueries: true
        });
    });
});
