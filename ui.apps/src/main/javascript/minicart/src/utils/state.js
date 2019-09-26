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

import React, { createContext, useContext, useReducer, useEffect } from 'react';
import { object, func } from 'prop-types';
import { useCookieValue } from '../utils/hooks';
import { useMutation } from '@apollo/react-hooks';

import MUTATION_CREATE_CART from '../queries/mutation_create_guest_cart.graphql';

export const CartContext = createContext();

export const CartProvider = ({ reducer, initialState, children }) => (
    <CartContext.Provider value={useReducer(reducer, initialState)}>{children}</CartContext.Provider>
);

CartProvider.propTypes = {
    reducer: func.isRequired,
    initialState: object.isRequired
};

export const useCartState = () => {
    const ctx = useContext(CartContext);
    const [{ cartId: stateCartId }, dispatch] = ctx;

    const cookieName = 'cif.cart';
    let [cartId, setCartCookie] = useCookieValue(cookieName);
    const [createCart, { data, error }] = useMutation(MUTATION_CREATE_CART);

    useEffect(() => {
        if (!cartId || cartId.length === 0) {
            createCart();
        }
    }, [cartId]);

    useEffect(() => {
        if (cartId && (!stateCartId || stateCartId.length === 0)) {
            dispatch({ type: 'cartId', cartId: cartId });
        }
    }, [cartId, stateCartId]);

    useEffect(() => {
        if (data) {
            setCartCookie(data.createEmptyCart);
            dispatch({ type: 'cartId', cartId: data.createEmptyCart });
        }
        // Could not create a new cart. TODO: What should be done in this case?
        if (error) {
            console.error(error);
        }
    }, [data, error]);

    return ctx;
};
