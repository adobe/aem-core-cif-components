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
import { useMutation, useLazyQuery } from '@apollo/react-hooks';

import MUTATION_GENERATE_TOKEN from '../queries/mutation_generate_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../queries/query_customer_details.graphql';
import MUTATION_REVOKE_TOKEN from '../queries/mutation_revoke_customer_token.graphql';

const UserContext = React.createContext();

const parseError = error => {
    if (error.networkError) {
        return error.networkError;
    }

    if (error.graphQLErrors && error.graphQLErrors.length > 0) {
        return error.graphQLErrors[0].message;
    }

    return JSON.stringify(error);
};

const UserContextProvider = props => {
    const [userToken, setUserToken] = useCookieValue('cif.userToken');
    const [token, setToken] = useState(userToken);
    const isSignedIn = () => !!userToken;

    const initialState = {
        currentUser: {
            firstname: '',
            lastname: '',
            email: ''
        },
        isSignedIn: isSignedIn(),
        signInError: ''
    };

    const [userState, setUserState] = useState(initialState);

    const [generateCustomerToken, { data, error }] = useMutation(MUTATION_GENERATE_TOKEN, {
        onError: error => {
            let signInError = parseError(error);
            setUserState({ ...userState, signInError });
        }
    });
    const [
        getCustomerDetails,
        { data: customerData, error: customerDetailsError, loading: customerDetailsLoading }
    ] = useLazyQuery(QUERY_CUSTOMER_DETAILS);

    const [
        revokeCustomerToken,
        { data: revokeTokenData, error: revokeTokenError, loading: revokeTokenLoading }
    ] = useMutation(MUTATION_REVOKE_TOKEN);

    // if the token changed, retrieve the user details for that token
    useEffect(() => {
        if (token.length > 0) {
            getCustomerDetails({
                context: { headers: { authorization: `Bearer ${token && token.length > 0 ? token : ''}` } }
            });
        }
    }, [token]);

    // if we have customer data (i.e. the getCustomerDetails query returned something) set it in the state
    useEffect(() => {
        if (customerData && customerData.customer && !customerDetailsError) {
            const { firstname, lastname, email } = customerData.customer;
            setUserState({ ...userState, currentUser: { firstname, lastname, email } });
        }
    }, [customerData, customerDetailsError, customerDetailsLoading]);

    // if the signin mutation returned something handle the response
    useEffect(() => {
        if (data && data.generateCustomerToken && !error) {
            setUserToken(data.generateCustomerToken.token);
            setToken(data.generateCustomerToken.token);
            setUserState({ ...userState, isSignedIn: true });
        }
    }, [data, error]);

    useEffect(() => {
        if (revokeTokenData && revokeTokenData.revokeCustomerToken && revokeTokenData.revokeCustomerToken.result) {
            setToken('');
            setUserToken('', 0);
            setUserState({ ...userState, isSignedIn: false });
        }
        if (revokeTokenError) {
            console.error(revokeTokenError.message);
        }
    }, [revokeTokenData, revokeTokenError, revokeTokenLoading]);

    const signIn = useCallback(
        ({ email, password }) => {
            generateCustomerToken({ variables: { email, password } });
        },
        [generateCustomerToken]
    );

    const signOut = useCallback(() => {
        revokeCustomerToken({
            context: { headers: { authorization: `Bearer ${token && token.length > 0 ? token : ''}` } }
        });
    }, [revokeCustomerToken, token]);

    const { children } = props;
    const contextValue = [
        userState,
        {
            signIn,
            signOut
        }
    ];
    return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>;
};
export default UserContextProvider;

export const useUserContext = () => useContext(UserContext);
