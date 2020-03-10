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
import React, { useContext, useState, useCallback } from 'react';
import { useCookieValue } from '../utils/hooks';
import { useMutation, useLazyQuery } from '@apollo/react-hooks';
import parseError from '../utils/parseError';
import { useAwaitQuery } from '../utils/hooks';

import MUTATION_GENERATE_TOKEN from '../queries/mutation_generate_token.graphql';
import MUTATION_REVOKE_TOKEN from '../queries/mutation_revoke_customer_token.graphql';
import MUTATION_CREATE_CUSTOMER from '../queries/mutation_create_customer.graphql';
import QUERY_CUSTOMER_DETAILS from '../queries/query_customer_details.graphql';
import QUERY_CUSTOMER_CART from '../queries/query_customer_cart.graphql';

const UserContext = React.createContext();

const UserContextProvider = props => {
    const [userCookie, setUserCookie] = useCookieValue('cif.userToken');
    const [, setCartCookie] = useCookieValue('cif.cart');
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
        isCreatingCustomer: false,
        cartId: null
    };

    const [userState, setUserState] = useState(initialState);

    const [generateCustomerToken] = useMutation(MUTATION_GENERATE_TOKEN);
    const [revokeCustomerToken] = useMutation(MUTATION_REVOKE_TOKEN);

    const [createCustomer] = useMutation(MUTATION_CREATE_CUSTOMER);

    const getCustomerCart = useAwaitQuery(QUERY_CUSTOMER_CART);
    const getCustomerDetails = useAwaitQuery(QUERY_CUSTOMER_DETAILS);

    const signIn = useCallback(
        ({ email, password }) => {
            (async function fetchToken(e, p) {
                try {
                    //1. generate the user's token.
                    const { data } = await generateCustomerToken({ variables: { email: e, password: p } });
                    setUserCookie(data.generateCustomerToken.token);
                    console.log(`Customer token?`, data);
                    //2. read the customer's details
                    const { data: customerData } = await getCustomerDetails({ fetchPolicy: 'no-cache' });
                    console.log(`User details? `, customerData);
                    const { firstname, lastname, email } = customerData.customer;

                    //3. read the users cart details
                    const { data: customerCartData } = await getCustomerCart();
                    console.log(`Cart? `, customerCartData);
                    setUserState({
                        ...userState,
                        isSignedIn: true,
                        currentUser: { firstname, lastname, email },
                        inProgress: false,
                        cartId: customerCartData.customerCart.id,
                        token: data.generateCustomerToken.token
                    });
                } catch (error) {
                    console.error('An error occurred during sign-in', error);
                    let signInError = parseError(error);
                    setUserState({ ...userState, signInError, isSignedIn: false, inProgress: false });
                }
            })(email, password);
        },
        [generateCustomerToken, getCustomerDetails, getCustomerCart]
    );

    const signOut = useCallback(() => {
        async function revokeToken() {
            try {
                await revokeCustomerToken();
                setCartCookie('', 0);
                setUserCookie('', 0);
                setUserState({
                    ...userState,
                    currentUser: {
                        firstname: '',
                        lastname: '',
                        email: ''
                    },
                    isSignedIn: false,
                    token: '',
                    cartId: null
                });
            } catch (error) {
                console.error('An error occurred during sign-out', error);
            }
        }

        revokeToken();
    }, [revokeCustomerToken, userState.token]);

    const resetPassword = async email => {
        await Promise.resolve(email);
    };

    const createAccount = useCallback(
        ({ email, password, firstname, lastname }) => {
            async function createAccount({ email, password, firstname, lastname }) {
                try {
                    console.log(`Create account...`);
                    const { data } = await createCustomer({ variables: { email, password, firstname, lastname } });
                    setUserState({ ...userState, isCreatingCustomer: false });
                    signIn({ email: data.createCustomer.customer.email, password });
                    // clear the password from the state
                } catch (error) {
                    let createAccountError = parseError(error);
                    setUserState({ ...userState, createAccountError });
                }
            }

            createAccount({ email, password, firstname, lastname });
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
