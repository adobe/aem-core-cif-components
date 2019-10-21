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
import { render, fireEvent, waitForElement } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';

import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../../queries/query_customer_details.graphql';
import MUTATION_REVOKE_TOKEN from '../../queries/mutation_revoke_customer_token.graphql';
import MUTATION_CREATE_CUSTOMER from '../../queries/mutation_create_customer.graphql';

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
        },
        {
            request: {
                query: MUTATION_REVOKE_TOKEN
            },
            result: {
                data: {
                    revokeCustomerToken: {
                        result: true
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
            const [{ isSignedIn }, { signOut }] = useUserContext();
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

        // normally the browser just removes a cookie with Max-Age=0
        //...but we're not in a browser
        const expectedCookieValue = 'cif.userToken=;path=/; domain=localhost;Max-Age=0';
        expect(document.cookie).toEqual(expectedCookieValue);
    });

    it('creates a customer and signs in', () => {
        const mocks = [
            {
                request: {
                    query: MUTATION_CREATE_CUSTOMER,
                    variables: {
                        firstname: 'Iris',
                        lastname: 'McCoy',
                        email: 'imccoy@weretail.net',
                        password: 'imccoy123'
                    }
                },
                result: {
                    data: {
                        createCustomer: {
                            customer: {
                                email: 'donnie@google.com',
                                firstname: 'Donnie',
                                lastname: 'Darko'
                            }
                        }
                    }
                }
            },
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
            }
        ];

        const ContextWrapper = () => {
            const [{ isSignedIn }, { createAccount }] = useUserContext();
            let content;

            const handleCreateAccount = () => {
                const data = {
                    firstname: 'Iris',
                    lastname: 'McCoy',
                    email: 'imccoy@weretail.net',
                    password: 'imccoy123'
                };

                createAccount(data);
            };

            if (isSignedIn) {
                content = (
                    <div data-testid="success">
                        <span>Signed in</span>
                    </div>
                );
            } else {
                content = (
                    <button data-testid="create-account" onClick={handleCreateAccount}>
                        Create account
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const { getByTestId } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <ContextWrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        expect(getByTestId('create-account')).not.toBeUndefined();

        fireEvent.click(getByTestId('create-account'));

        const successMessage = waitForElement(() => {
            return getByTestId('success');
        });

        expect(successMessage).not.toBeUndefined();
    });
});
