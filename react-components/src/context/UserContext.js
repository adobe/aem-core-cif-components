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
            case 'setNewAddress':
                return {
                    ...state,
                    currentUser: {
                        ...state.currentUser,
                        addresses: [...state.currentUser.addresses, action.address]
                    }
                };
            case 'setAddressFormError':
                return {
                    ...state,
                    addressFormError: action.error
                };
            case 'clearAddressFormError':
                return {
                    ...state,
                    addressFormError: null
                };
            case 'beginEditingAddress':
                return {
                    ...state,
                    updateAddress: action.address
                };
            case 'endEditingAddress':
                return {
                    ...state,
                    updateAddress: null
                };
            case 'updateAddresses':
                return {
                    ...state,
                    currentUser: {
                        ...state.currentUser,
                        addresses: [...state.currentUser.addresses].map(address => {
                            return address.id === action.address.id ? action.address : address;
                        })
                    }
                };
            case 'beginDeletingAddress':
                return {
                    ...state,
                    deleteAddress: action.address
                };
            case 'endDeletingAddress':
                return {
                    ...state,
                    deleteAddress: null
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
                    isAccountDropdownOpen: false,
                    inProgress: false,
                    token: '',
                    currentUser: {
                        firstname: '',
                        lastname: '',
                        email: '',
                        addresses: []
                    },
                    cartId: '',
                    accountDropdownView: 'SIGN_IN'
                };
            case 'toggleAccountDropdown':
                return {
                    ...state,
                    isAccountDropdownOpen: action.toggle
                };
            case 'changeAccountDropdownView':
                return {
                    ...state,
                    accountDropdownView: action.view
                };
            case 'openAddressForm':
                return {
                    ...state,
                    isShowAddressForm: true
                };
            case 'closeAddressForm':
                return {
                    ...state,
                    isShowAddressForm: false
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
    const accountContainerQuerySelector =
        (props.config && props.config.accountContainerQuerySelector) || '.header__accountTrigger #miniaccount';
    const addressBookContainerQuerySelector =
        (props.config && props.config.addressBookContainerQuerySelector) || '#addressbook';
    const addressBookPath = (props.config && props.config.addressBookPath) || '/my-account/address-book.html';

    const initialState = props.initialState || {
        currentUser: {
            firstname: '',
            lastname: '',
            email: '',
            addresses: []
        },
        token: userCookie,
        isSignedIn: isSignedIn(),
        isAccountDropdownOpen: false,
        isShowAddressForm: false,
        addressFormError: null,
        updateAddress: null,
        deleteAddress: null,
        signInError: null,
        inProgress: false,
        createAccountError: null,
        createAccountEmail: null,
        cartId: null,
        accountDropdownView: 'SIGN_IN',
        accountContainerQuerySelector,
        addressBookContainerQuerySelector,
        addressBookPath
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

    const toggleAccountDropdown = toggle => {
        dispatch({ type: 'toggleAccountDropdown', toggle });
    };

    const showSignIn = () => {
        dispatch({ type: 'changeAccountDropdownView', view: 'SIGN_IN' });
    };

    const showMyAccount = () => {
        dispatch({ type: 'changeAccountDropdownView', view: 'MY_ACCOUNT' });
    };

    const showForgotPassword = () => {
        dispatch({ type: 'changeAccountDropdownView', view: 'FORGOT_PASSWORD' });
    };

    const showCreateAccount = () => {
        dispatch({ type: 'changeAccountDropdownView', view: 'CREATE_ACCOUNT' });
    };

    const showAccountCreated = () => {
        dispatch({ type: 'changeAccountDropdownView', view: 'ACCOUNT_CREATED' });
    };

    const showChangePassword = () => {
        dispatch({ type: 'changeAccountDropdownView', view: 'CHANGE_PASSWORD' });
    };

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
            resetCustomerCart,
            toggleAccountDropdown,
            showSignIn,
            showMyAccount,
            showForgotPassword,
            showCreateAccount,
            showAccountCreated,
            showChangePassword
        }
    ];
    return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>;
};

UserContextProvider.propTypes = {
    config: object,
    initialState: object
};
export default UserContextProvider;

export const useUserContext = () => useContext(UserContext);
