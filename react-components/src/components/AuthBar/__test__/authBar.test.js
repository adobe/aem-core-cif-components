/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
import { render } from 'test-utils';

import AuthBar from '../authBar';
import UserContextProvider from '../../../context/UserContext';

describe('<AuthBar>', () => {
    it('renders the component for anonymous user', () => {
        const handler = jest.fn(state => state);

        const { asFragment } = render(
            <UserContextProvider reducerFactory={() => handler}>
                <AuthBar />
            </UserContextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component for logged user', () => {
        const handler = jest.fn(state => state);

        const initialState = {
            currentUser: {
                firstname: 'Test',
                lastname: 'User',
                email: 'test_user@example.com',
                addresses: []
            },
            isSignedIn: true
        };

        const { asFragment } = render(
            <UserContextProvider initialState={initialState} reducerFactory={() => handler}>
                <AuthBar />
            </UserContextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
