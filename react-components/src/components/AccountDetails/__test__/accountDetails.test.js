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
import { render, screen, waitForElement } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';

import AccountDetails from '../';
import UserContextProvider from '../../../context/UserContext';
import getDetailsQuery from '../query_get_customer_information.graphql';
import ConfigContextProvider from '../../../context/ConfigContext';

describe('<AccountDetails>', () => {
    const withContext = (Component, userContext = {}, mocks = []) => {
        return (
            <ConfigContextProvider config={{ mountingPoints: { accountdetails: 'mock' } }}>
                <MockedProvider mocks={mocks} addTypename={false}>
                    <UserContextProvider initialState={userContext}>
                        <Component />
                    </UserContextProvider>
                </MockedProvider>
            </ConfigContextProvider>
        );
    };

    it('renders the Account Details data for an unauthenticated user', () => {
        const { queryByText } = render(withContext(AccountDetails, { isSignedIn: false }));

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
        const { queryByText } = render(withContext(AccountDetails, { isSignedIn: true }, mocks));

        await waitForElement(() => screen.getByLabelText('name'));

        expect(queryByText('Jane')).not.toBeUndefined();
        expect(queryByText('Doe')).not.toBeUndefined();
        expect(queryByText('jdoe@gmail.com')).not.toBeUndefined();
    });
});
