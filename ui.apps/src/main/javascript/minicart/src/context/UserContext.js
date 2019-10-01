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

const UserContext = React.createContext();

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

    console.log(`Do we have a token? ${token}`);
    const [userState, setUserState] = useState(initialState);

    const [generateCustomerToken, { data, error }] = useMutation(MUTATION_GENERATE_TOKEN);
    const [getCustomerDetails, { data: customerData, error: customerDetailsError }] = useLazyQuery(
        QUERY_CUSTOMER_DETAILS
    );

    useEffect(() => {
        console.log(`Retrieve user details with token ${token}`);
        getCustomerDetails({
            context: { headers: { authorization: `Bearer ${token && token.length > 0 ? token : ''}` } }
        });
    }, [token]);

    useEffect(() => {
        console.log(`Got customer data `, customerData);
        if (customerData && customerData.customer && !customerDetailsError) {
            const { firstname, lastname, email } = customerData.customer;
            setUserState({ ...userState, currentUser: { firstname, lastname, email } });
        }
    }, [customerData, customerDetailsError]);

    useEffect(() => {
        if (data && data.generateCustomerToken && !error) {
            console.log(`Signed in successfully ${data.generateCustomerToken.token}`);
            setUserToken(data.generateCustomerToken.token);
            setToken(data.generateCustomerToken.token);
            setUserState({ ...userState, isSignedIn: true });
        }

        if (error) {
            let signInError = error.message ? error.message : JSON.stringify(error);
            setUserState({ ...userState, signInError });
        }
    }, [data, error]);

    const signIn = useCallback(
        ({ email, password }) => {
            generateCustomerToken({ variables: { email, password } });
        },
        [generateCustomerToken]
    );

    const signOut = useCallback(() => {
        setToken('');
        setUserToken('');
        setUserState({ ...userState, isSignedIn: false });
    });

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
