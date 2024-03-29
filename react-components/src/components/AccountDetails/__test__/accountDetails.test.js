/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import React from 'react';
import { screen, waitForElement } from '@testing-library/react';
import { render } from 'test-utils';
import AccountDetails from '../accountDetails';

const config = {
    storeView: 'default',
    graphqlEndpoint: 'none',
    graphqlMethod: 'GET',
    mountingPoints: { accountDetails: 'mock' }
};

describe('<AccountDetails>', () => {
    it('renders the Account Details data for an unauthenticated user', () => {
        const { queryByText } = render(<AccountDetails />, { config: config, userContext: { isSignedIn: false } });

        expect(queryByText('Please Sign in to see the account details.')).not.toBeUndefined();
    });

    it('renders the Account Details data for an authenticated user', async () => {
        const { queryByText } = render(<AccountDetails />, {
            config: config,
            userContext: { isSignedIn: true }
        });

        await waitForElement(() => screen.getByLabelText('name'));

        expect(queryByText('Jane')).not.toBeUndefined();
        expect(queryByText('Doe')).not.toBeUndefined();
        expect(queryByText('jdoe@gmail.com')).not.toBeUndefined();
    });
});
