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
import { render } from '@testing-library/react';

const { useConfigContext, default: ConfigContextProvider } = require('../ConfigContext');

describe('ConfigContext', () => {
    const Consumer = () => {
        const { storeView, graphqlEndpoint, graphqlMethod } = useConfigContext();

        return (
            <div>
                <span data-testid="storeView">{storeView}</span>
                <span data-testid="graphqlEndpoint">{graphqlEndpoint}</span>
                <span data-testid="graphqlMethod">{graphqlMethod}</span>
            </div>
        );
    };

    it('provides the configuration', () => {
        const { asFragment } = render(
            <ConfigContextProvider
                config={{ storeView: 'my-store', graphqlEndpoint: '/api/graphql', graphqlMethod: 'GET' }}>
                <Consumer />
            </ConfigContextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
