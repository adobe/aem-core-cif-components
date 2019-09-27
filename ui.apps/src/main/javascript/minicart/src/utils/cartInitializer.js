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
import { useEffect } from 'react';
import { useCartState } from './state';
import { useCookieValue } from './hooks';
import { useMutation } from '@apollo/react-hooks';

import MUTATION_CREATE_CART from '../queries/mutation_create_guest_cart.graphql';

const CartInitializer = props => {
    const [{ cartId: stateCartId }, dispatch] = useCartState();
    const CART_COOKIE = 'cif.cart';

    const [cartId, setCartCookie] = useCookieValue(CART_COOKIE);
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

    return props.children;
};

export default CartInitializer;
