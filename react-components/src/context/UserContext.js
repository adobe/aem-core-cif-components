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
import { object, func } from 'prop-types';

import { useCookieValue } from '../utils/hooks';
import { useMutation } from '@apollo/client';
import parseError from '../utils/parseError';
import { useAwaitQuery } from '../utils/hooks';
import {
    resetCustomerCart as resetCustomerCartAction,
    signOutUser as signOutUserAction,
    deleteAddress as deleteAddressAction
} from '../actions/user';

import MUTATION_DELETE_CUSTOMER_ADDRESS from '../queries/mutation_delete_customer_address.graphql';
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
            case 'setAddressFormError':
                return {
                    ...state,
                    addressFormError: parseError(action.error)
                };
            case 'clearAddressFormError':
                return {
                    ...state,
                    addressFormError: null
                };
            case 'postCreateAddress':
                return {
                    ...state,
                    currentUser: {
                        ...state.currentUser,
                        addresses: [
                            ...[...state.currentUser.addresses].map(address => {
                                if (action.resetFields) {
                                    Object.entries(action.resetFields).forEach(([key, value]) => {
                                        address[key] = value;
                                    });
                                }
                                return address;
                            }),
                            action.address
                        ]
                    },
                    addressFormError: null,
                    isShowAddressForm: false
                };
            case 'beginEditingAddress':
                return {
                    ...state,
                    updateAddress: action.address,
                    isShowAddressForm: true
                };
            case 'endEditingAddress':
                return {
                    ...state,
                    updateAddress: null
                };
            case 'postUpdateAddress':
                return {
                    ...state,
                    currentUser: {
                        ...state.currentUser,
                        addresses: [...state.currentUser.addresses].map(address => {
                            if (action.resetFields) {
                                Object.entries(action.resetFields).forEach(([key, value]) => {
                                    address[key] = value;
                                });
                            }
                            return address.id === action.address.id ? action.address : address;
                        })
                    },
                    updateAddress: null,
                    addressFormError: null,
                    isShowAddressForm: false
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
            case 'postDeleteAddress':
                return {
                    ...state,
                    currentUser: {
                        ...state.currentUser,
                        addresses: [...state.currentUser.addresses].filter(address => address.id !== action.address.id)
                    },
                    deleteAddress: null,
                    deleteAddressError: null
                };
            case 'deleteAddressError':
                return {
                    ...state,
                    deleteAddressError: parseError(action.error)
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
                        email: '',
                        addresses: []
                    },
                    cartId: '',
                    accountDropdownView: null
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

    const factory = props.reducerFactory || reducerFactory;
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
        updateAddressError: null,
        deleteAddress: null,
        deleteAddressError: null,
        signInError: null,
        inProgress: false,
        createAccountError: null,
        createAccountEmail: null,
        cartId: null,
        accountDropdownView: null
    };

    const [userState, dispatch] = useReducer(factory(), initialState);

    const [deleteCustomerAddress] = useMutation(MUTATION_DELETE_CUSTOMER_ADDRESS);
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

    const deleteAddress = async address => {
        await deleteAddressAction({ deleteCustomerAddress, address, dispatch });
    };

    const { children } = props;
    const contextValue = [
        userState,
        {
            dispatch,
            setToken,
            setError,
            signOut,
            setCustomerCart,
            getUserDetails,
            resetCustomerCart,
            toggleAccountDropdown,
            showSignIn,
            showMyAccount,
            showForgotPassword,
            showCreateAccount,
            showAccountCreated,
            showChangePassword,
            deleteAddress
        }
    ];
    return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>;
};

UserContextProvider.propTypes = {
    reducerFactory: func,
    initialState: object
};
export default UserContextProvider;

export const useUserContext = () => useContext(UserContext);
