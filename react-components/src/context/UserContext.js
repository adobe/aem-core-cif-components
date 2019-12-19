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
import React, { useContext, useState, useCallback, useEffect } from 'react';
import { useCookieValue } from '../utils/hooks';
import { useMutation, useApolloClient } from '@apollo/react-hooks';
import parseError from '../utils/parseError';

import MUTATION_GENERATE_TOKEN from '../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../queries/query_customer_details.graphql';
import MUTATION_REVOKE_TOKEN from '../queries/mutation_revoke_customer_token.graphql';
import MUTATION_CREATE_CUSTOMER from '../queries/mutation_create_customer.graphql';

const UserContext = React.createContext();

const UserContextProvider = props => {
    const [userCookie, setUserCookie] = useCookieValue('cif.userToken');
    const [password, setPassword] = useState(null);
    const isSignedIn = () => !!userCookie;

    const initialState = {
        currentUser: {
            firstname: '',
            lastname: '',
            email: ''
        },
        token: userCookie,
        isSignedIn: isSignedIn(),
        signInError: null,
        inProgress: false,
        createAccountError: null,
        isCreatingCustomer: false
    };
    const apolloClient = useApolloClient();
    const [userState, setUserState] = useState(initialState);

    const [generateCustomerToken, { data, error }] = useMutation(MUTATION_GENERATE_TOKEN, {
        onError: error => {
            let signInError = parseError(error);
            setUserState({ ...userState, signInError });
        }
    });

    const [
        revokeCustomerToken,
        { data: revokeTokenData, error: revokeTokenError, loading: revokeTokenLoading }
    ] = useMutation(MUTATION_REVOKE_TOKEN);

    const [
        createCustomer,
        { data: createCustomerData, error: createCustomerError, loading: createCustomerLoading }
    ] = useMutation(MUTATION_CREATE_CUSTOMER, {
        onError: error => {
            let createAccountError = parseError(error);
            setUserState({ ...userState, createAccountError });
        }
    });

    /*
     * We use the apollo client directly because of the way the responses are handled by the hooks.
     * Case in point: if we sign-out and then sign-in with a different user the data from the previous
     * query is still available.
     *
     * This will need refactoring the next iterations, maybe even split into higher-level hooks.
     */
    const getCustomerDetails = opts => {
        apolloClient
            .query({
                query: QUERY_CUSTOMER_DETAILS,
                fetchPolicy: 'no-cache',
                ...opts
            })
            .then(({ data, error }) => {
                if (data) {
                    const { firstname, lastname, email } = data.customer;
                    setUserState({
                        ...userState,
                        isSignedIn: true,
                        currentUser: { firstname, lastname, email },
                        inProgress: false
                    });
                }
                if (error) {
                    setUserState({ ...userState, isSignedIn: false, token: '' });
                    setUserCookie('', 0);
                }
            });
    };

    // if the token changed, retrieve the user details for that token
    useEffect(() => {
        if (userState.token.length > 0) {
            getCustomerDetails({
                context: { headers: { authorization: `Bearer ${userState.token && userState.token.length > 0 ? userState.token : ''}` } }
            });
            setUserState({ ...userState, inProgress: true });
        }
    }, [userState.token]);

    // if the signin mutation returned something handle the response
    useEffect(() => {
        if (data && data.generateCustomerToken && !error) {
            setUserCookie(data.generateCustomerToken.token);
            setUserState({ ...userState, signInError: null, createAccountError: null, token: data.generateCustomerToken.token });
        }
    }, [data, error]);

    useEffect(() => {
        if (revokeTokenData && revokeTokenData.revokeCustomerToken && revokeTokenData.revokeCustomerToken.result) {
            setUserCookie('', 0);
            setUserState({
                ...userState,
                currentUser: {
                    firstname: '',
                    lastname: '',
                    email: ''
                },
                isSignedIn: false,
                token: ''
            });
        }
        if (revokeTokenError) {
            console.error(revokeTokenError.message);
        }
    }, [revokeTokenData, revokeTokenError, revokeTokenLoading]);

    // after creating the customer we have to log them in
    useEffect(() => {
        if (createCustomerData && createCustomerData.createCustomer) {
            setUserState({ ...userState, isCreatingCustomer: false });
            signIn({ email: createCustomerData.createCustomer.customer.email, password });
            // clear the password from the state
            setPassword(null);
        }

        if (createCustomerError) {
            let errorMessage = parseError(createCustomerError);
            setUserState({ ...userState, createAccountError: errorMessage, isCreatingCustomer: false });
        }
        if (createCustomerLoading) {
            setUserState({ ...userState, isCreatingCustomer: true });
        }
    }, [createCustomerData, createCustomerError, createCustomerLoading]);

    const signIn = useCallback(
        ({ email, password }) => {
            generateCustomerToken({ variables: { email, password } });
        },
        [generateCustomerToken]
    );

    const signOut = useCallback(() => {
        revokeCustomerToken({
            context: { headers: { authorization: `Bearer ${userState.token && userState.token.length > 0 ? userState.token : ''}` } }
        });
    }, [revokeCustomerToken, userState.token]);

    const resetPassword = async email => {
        await Promise.resolve(email);
    };

    const createAccount = useCallback(
        ({ email, password, firstname, lastname }) => {
            createCustomer({ variables: { email, password, firstname, lastname } });
            setPassword(password);
        },
        [createCustomer]
    );

    const { children } = props;
    const contextValue = [
        userState,
        {
            signIn,
            signOut,
            resetPassword,
            createAccount
        }
    ];
    return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>;
};
export default UserContextProvider;

export const useUserContext = () => useContext(UserContext);
