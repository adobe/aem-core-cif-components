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
import { useState } from 'react';
import { useUserContext } from '../../context/UserContext';
import { useMutation } from '@apollo/client';
import { useCartState } from '../Minicart/cartContext';
import { useAwaitQuery, useCookieValue } from '../../utils/hooks';
import { mergeCarts } from '../../actions/cart';

import MUTATION_MERGE_CARTS from '../../queries/mutation_merge_carts.graphql';
import QUERY_CUSTOMER_CART from '../../queries/query_customer_cart.graphql';
import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';
import QUERY_CART_DETAILS from '../../queries/query_cart_details.graphql';

export const useSignin = props => {
    const { showMyAccount } = props;
    const [{ cartId }, cartDispatch] = useCartState();
    const [userState, { setToken, getUserDetails, setCustomerCart, setError }] = useUserContext();
    const [inProgress, setInProgress] = useState(false);

    const [, setCartCookie] = useCookieValue('cif.cart');

    const [mergeCartsMutation] = useMutation(MUTATION_MERGE_CARTS);
    const fetchCustomerCart = useAwaitQuery(QUERY_CUSTOMER_CART);
    const cartDetailsQuery = useAwaitQuery(QUERY_CART_DETAILS);
    const [generateCustomerToken] = useMutation(MUTATION_GENERATE_TOKEN);

    let errorMessage = '';
    if (userState.signInError && userState.signInError.length > 0) {
        errorMessage = userState.signInError;
    }

    const handleSubmit = async ({ email, password }) => {
        setInProgress(true);
        try {
            // 1. generate the customer token
            const { data } = await generateCustomerToken({ variables: { email, password } });
            const token = data.generateCustomerToken.token;
            setToken(token);

            // 2. get the user details
            await getUserDetails();

            const { data: customerCartData } = await fetchCustomerCart({
                fetchPolicy: 'network-only'
            });
            const customerCartId = customerCartData.customerCart.id;

            // 3. merge the shopping cart if necessary
            let mergedCartId;

            if (cartId) {
                mergedCartId = await mergeCarts({
                    mergeCartsMutation,
                    cartDetailsQuery,
                    cartId,
                    customerCartId,
                    dispatch: cartDispatch
                });
            } else {
                mergedCartId = customerCartId;
            }
            //4. set the cart id in the cookie
            setCartCookie(mergedCartId);
            setCustomerCart(mergedCartId);

            //5. show my account view in account dropdown or navigation side panel after sign in
            showMyAccount();
        } catch (e) {
            setError(e);
            setInProgress(false);
        }
    };

    return {
        inProgress,
        handleSubmit,
        errorMessage,
        isSignedIn: userState.isSignedIn
    };
};
