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
import { useMutation } from '@apollo/react-hooks';
import { useState } from 'react';
import { useAwaitQuery, useCookieValue } from '../../utils/hooks';
import { useUserContext } from '../../context/UserContext';
import { useCartState } from '../Minicart/cartContext';

import MUTATION_MERGE_CARTS from '../../queries/mutation_merge_carts.graphql';
import QUERY_CUSTOMER_CART from '../../queries/query_customer_cart.graphql';
import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';
import MUTATION_CREATE_CUSTOMER from '../../queries/mutation_create_customer.graphql';

export default () => {
    const [{ cartId }, cartDispatch] = useCartState();
    const [, setUserCookie] = useCookieValue('cif.userToken');
    const [, setCartCookie] = useCookieValue('cif.cart');

    const [{ isSignedIn, createAccountError }, { dispatch, setToken }] = useUserContext();
    const [inProgress, setInProgress] = useState(false);

    const [mergeCarts] = useMutation(MUTATION_MERGE_CARTS);
    const fetchCustomerCart = useAwaitQuery(QUERY_CUSTOMER_CART);
    const [createCustomer] = useMutation(MUTATION_CREATE_CUSTOMER);
    const [generateCustomerToken] = useMutation(MUTATION_GENERATE_TOKEN);

    const handleCreateAccount = async formValues => {
        setInProgress(true);
        const {
            customer: { email, firstname, lastname },
            password
        } = formValues;
        try {
            const {
                data: {
                    createCustomer: { customer }
                }
            } = await createCustomer({
                variables: { email, password, firstname, lastname }
            });

            //2. Generate the customer token.
            // Most of the commerce solutions DO NOT sign in the user after the account is created.
            const { data: customerTokenData } = await generateCustomerToken({
                variables: { email: customer.email, password }
            });
            const token = customerTokenData.generateCustomerToken.token;

            //3. Set the token in the cookie now because subsequent requests would need it
            setUserCookie(token);
            setToken(token);

            const { data: customerCartData } = await fetchCustomerCart();
            const customerCartId = customerCartData.customerCart.id;

            //4. Merge the shopping cart
            const { data: mergeCartsData } = await mergeCarts({
                variables: {
                    sourceCartId: cartId,
                    destinationCartId: customerCartId
                }
            });
            const mergedCartId = mergeCartsData.mergeCarts.id;

            //5. Dispatch the action to update the user state
            setCartCookie(mergedCartId);
            cartDispatch({ type: 'cartId', cartId: mergedCartId });
            dispatch({ type: 'postCreateAccount', token, currentUser: customer, cartId: mergedCartId });
        } catch (error) {
            dispatch({ type: 'createAccountError', error });
        } finally {
            setInProgress(false);
        }
    };

    return [{ isSignedIn, createAccountError, inProgress }, { createAccount: handleCreateAccount }];
};
