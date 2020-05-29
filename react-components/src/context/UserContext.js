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
import React, { useContext, useReducer, useCallback } from 'react';
import { object } from 'prop-types';

import { useCookieValue } from '../utils/hooks';
import { useMutation } from '@apollo/react-hooks';
import parseError from '../utils/parseError';
import { useAwaitQuery } from '../utils/hooks';
import { resetCustomerCart as resetCustomerCartAction, signOutUser as signOutUserAction } from '../actions/user';

import MUTATION_REVOKE_TOKEN from '../queries/mutation_revoke_customer_token.graphql';
import QUERY_CUSTOMER_DETAILS from '../queries/query_customer_details.graphql';

const UserContext = React.createContext();

const reducerFactory = () => {
    return (state, action) => {
        switch (action.type) {
            case 'setUserDetails':
                return {
                    ...state,
                    inProgress: false,
                    currentUser: {
                        ...action.userDetails
                    }
                };
            case 'setCartId':
                return {
                    ...state,
                    cartId: action.cartId
                };
            case 'setInProgress': {
                return {
                    ...state,
                    inProgress: true
                };
            }
            case 'setToken':
                return {
                    ...state,
                    isSignedIn: true,
                    inProgress: false,
                    token: action.token,
                    signInError: null
                };
            case 'postCreateAccount':
                return {
                    ...state,
                    isSignedIn: false,
                    inProgress: false,
                    createAccountError: null,
                    createAccountEmail: action.accountEmail
                };
            case 'cleanupAccountCreated':
                return {
                    ...state,
                    createAccountEmail: null
                };
            case 'error': {
                return {
                    ...state,
                    inProgress: false,
                    signInError: action.error
                };
            }
            case 'createAccountError': {
                return {
                    ...state,
                    inProgress: false,
                    createAccountError: parseError(action.error)
                };
            }
            case 'signOut':
                return {
                    ...state,
                    isSignedIn: false,
                    inProgress: false,
                    token: '',
                    currentUser: {
                        firstname: '',
                        lastname: '',
                        email: ''
                    },
                    cartId: ''
                };
            default:
                return state;
        }
    };
};

const UserContextProvider = props => {
    const [userCookie, setUserCookie] = useCookieValue('cif.userToken');
    const [, setCartCookie] = useCookieValue('cif.cart');
    const isSignedIn = () => !!userCookie;

    const initialState = props.initialState || {
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
        createAccountEmail: null,
        cartId: null
    };

    const [userState, dispatch] = useReducer(reducerFactory(), initialState);

    const [revokeCustomerToken] = useMutation(MUTATION_REVOKE_TOKEN);
    const fetchCustomerDetails = useAwaitQuery(QUERY_CUSTOMER_DETAILS);

    const setToken = token => {
        setUserCookie(token);
        dispatch({ type: 'setToken', token });
    };

    const setError = error => {
        dispatch({ type: 'error', error: parseError(error) });
    };

    const setCustomerCart = cartId => {
        dispatch({ type: 'setCartId', cartId });
    };

    const signOut = async () => {
        await signOutUserAction({ revokeCustomerToken, setCartCookie, setUserCookie, dispatch });
    };

    const resetCustomerCart = async fetchCustomerCartQuery => {
        await resetCustomerCartAction({ fetchCustomerCartQuery, dispatch });
    };

    const resetPassword = async email => {
        await Promise.resolve(email);
    };

    const getUserDetails = useCallback(async () => {
        try {
            const { data: customerData } = await fetchCustomerDetails({ fetchPolicy: 'no-cache' });
            dispatch({ type: 'setUserDetails', userDetails: customerData.customer });
        } catch (error) {
            dispatch({ type: 'error', error });
        }
    }, [fetchCustomerDetails]);

    const { children } = props;
    const contextValue = [
        userState,
        {
            dispatch,
            setToken,
            setError,
            signOut,
            resetPassword,
            setCustomerCart,
            getUserDetails,
            resetCustomerCart
        }
    ];
    return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>;
};

UserContextProvider.propTypes = {
    initialState: object
};
export default UserContextProvider;

export const useUserContext = () => useContext(UserContext);
