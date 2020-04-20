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
import { useCartState } from './cartContext';
import { useCookieValue, useAwaitQuery } from '../../utils/hooks';
import { useMutation } from '@apollo/react-hooks';
import { useUserContext } from '../../context/UserContext';

import { removeItemFromCart, addCoupon, removeCoupon } from '../../actions/cart';

import MUTATION_REMOVE_ITEM from '../../queries/mutation_remove_item.graphql';
import MUTATION_ADD_COUPON from '../../queries/mutation_add_coupon.graphql';
import MUTATION_REMOVE_COUPON from '../../queries/mutation_remove_coupon.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

const CartInitializer = props => {
    const [{ cartId: stateCartId }, dispatch] = useCartState();
    const [{ cartId: registeredCartId }] = useUserContext();

    const CART_COOKIE = 'cif.cart';

    const [cartId, setCartCookie] = useCookieValue(CART_COOKIE);

    const [removeItemMutation] = useMutation(MUTATION_REMOVE_ITEM);
    const [addCouponMutation] = useMutation(MUTATION_ADD_COUPON);
    const [removeCouponMutation] = useMutation(MUTATION_REMOVE_COUPON);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);

    const createCartHandlers = (cartId, dispatch) => {
        return {
            removeItem: async itemId => {
                dispatch({ type: 'beginLoading' });
                await removeItemFromCart({ cartId, itemId, dispatch, cartDetailsQuery, removeItemMutation });
                dispatch({ type: 'endLoading' });
            },
            addCoupon: async couponCode => {
                dispatch({ type: 'beginLoading' });
                await addCoupon({
                    cartId,
                    couponCode,
                    cartDetailsQuery,
                    addCouponMutation,
                    dispatch
                });

                dispatch({ type: 'endLoading' });
            },
            removeCoupon: async () => {
                dispatch({ type: 'beginLoading' });
                await removeCoupon({ cartId, removeCouponMutation, cartDetailsQuery, dispatch });
                dispatch({ type: 'endLoading' });
            }
        };
    };

    useEffect(() => {
        if (cartId && cartId.length > 0 && !stateCartId) {
            console.log(`Put the cart id ${cartId} in the state.`);
            dispatch({ type: 'cartId', cartId, methods: createCartHandlers(cartId, dispatch) });
        }
    }, [cartId]);

    useEffect(() => {
        if (stateCartId && (!cartId || cartId.length === 0)) {
            console.log(`Put the cart id in the cookie`);
            setCartCookie(stateCartId);
        }
    }, [stateCartId]);

    useEffect(() => {
        if (registeredCartId) {
            console.log(`Running the effect with the registered cart id ${registeredCartId}`);
            setCartCookie(registeredCartId);
            dispatch({
                type: 'cartId',
                cartId: registeredCartId,
                methods: createCartHandlers(registeredCartId, dispatch)
            });
        }
    }, [registeredCartId]);

    return props.children;
};

export default CartInitializer;
