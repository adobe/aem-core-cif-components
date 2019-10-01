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
import { render, fireEvent, waitForElement, getByTestId } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';

import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../../queries/query_customer_details.graphql';

import UserContextProvider, { useUserContext } from '../UserContext';

describe('UserContext test', () => {
    const mocks = [
        {
            request: {
                query: MUTATION_GENERATE_TOKEN
            },
            result: {
                data: {
                    generateCustomerToken: {
                        token: 'token123'
                    }
                }
            }
        },
        {
            request: {
                query: QUERY_CUSTOMER_DETAILS
            },
            result: {
                data: {
                    customer: {
                        email: 'test@example.com',
                        firstname: 'John',
                        lastname: 'Doe'
                    }
                }
            }
        }
    ];
    it('performs a sign in', async () => {
        const ContextWrapper = () => {
            const [{ currentUser, isSignedIn }, { signIn }] = useUserContext();

            const display = `${currentUser.firstname} ${currentUser.lastname}`;

            let content;
            if (isSignedIn) {
                content = <div data-testid="success">{display}</div>;
            } else {
                content = <button onClick={() => signIn('', '')}>Sign in</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('John Doe');
    });

    it('performs a sign out', async () => {
        const ContextWrapper = () => {
            const [{ currentUser, isSignedIn }, { signOut }] = useUserContext();
            let content;
            if (isSignedIn) {
                content = (
                    <div>
                        <span>Signed in</span>
                        <button onClick={() => signOut()}>{'Sign out'}</button>
                    </div>
                );
            } else {
                content = <div data-testid="success">{'Signed out'}</div>;
            }

            return <div>{content}</div>;
        };

        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.userToken=token123'
        });

        const { getByRole, getByTestId, getByText } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByText('Signed in')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));

        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
    });
});
