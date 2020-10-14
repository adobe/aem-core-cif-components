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
import React from 'react';
import { screen, waitForElement } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import AccountDetails from '../accountDetails';
import getDetailsQuery from '../../../queries/query_get_customer_information.graphql';

const config = {
    storeView: 'default',
    graphqlEndpoint: 'none',
    mountingPoints: { accountDetails: 'mock' }
};

describe('<AccountDetails>', () => {
    it('renders the Account Details data for an unauthenticated user', () => {
        const { queryByText } = render(<AccountDetails />, { config: config, userContext: { isSignedIn: false } });

        expect(queryByText('Please Sign in to see the account details.')).not.toBeUndefined();
    });

    it('renders the Account Details data for an authenticated user', async () => {
        const mocks = [
            {
                request: {
                    query: getDetailsQuery
                },
                result: {
                    data: { customer: { id: 1, firstname: 'Jane', lastname: 'Doe', email: 'jdoe@gmail.com' } }
                }
            }
        ];
        const { queryByText } = render(<AccountDetails />, {
            config: config,
            userContext: { isSignedIn: true },
            mocks: mocks
        });

        await waitForElement(() => screen.getByLabelText('name'));

        expect(queryByText('Jane')).not.toBeUndefined();
        expect(queryByText('Doe')).not.toBeUndefined();
        expect(queryByText('jdoe@gmail.com')).not.toBeUndefined();
    });
});
