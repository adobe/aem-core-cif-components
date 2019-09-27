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
import { useMutation } from '@apollo/react-hooks';

import MUTATION_GENERATE_TOKEN from '../queries/mutation_generate_token.graphql';

const UserContext = React.createContext();

const [userToken, setUserToken] = useCookieValue('userToken');

const UserContextProvider = props => {
    const initialState = {
        currentUser: {
            firstname: '',
            lastname: '',
            email: ''
        },
        isSignedIn: !!userToken,
        signInError: ''
    };

    const [userState, setUserState] = useState(initialState);

    const [generateCustomerToken, { data, error }] = useMutation(MUTATION_GENERATE_TOKEN);

    const signIn = useCallback(
        ({ email, password }) => {
            generateCustomerToken({ variables: { email, password } });
        },
        [generateCustomerToken]
    );

    useEffect(() => {
        if (data && !error) {
            setUserToken(data.generateCustomerToken.token);
            setUserState({ ...userState, isSignedIn: true });
        }

        if (error) {
            let signInError = error.message ? error.message : JSON.stringify(error);
            setUserState({ ...userState, signInError });
        }
    }, [data, error]);

    const { children } = props;
    const contextValue = [
        userState,
        {
            signIn
        }
    ];
    return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>;
};
export default UserContextProvider;

export const useUserContext = () => useContext(UserContext);
