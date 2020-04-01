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

import parseError from '../../utils/parseError';

import { removeItemFromCart } from '../../actions/cart';

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
    const [addCoupon] = useMutation(MUTATION_ADD_COUPON);
    const [removeCoupon] = useMutation(MUTATION_REMOVE_COUPON);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);

    const createCartHandlers = (cartId, dispatch) => {
        return {
            removeItem: async itemId => {
                await removeItemFromCart({ cartId, itemId, dispatch, cartDetailsQuery, removeItemMutation });
            },
            addCoupon: couponCode => {
                dispatch({ type: 'beginLoading' });
                return addCoupon({
                    variables: { cartId, couponCode },
                    refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }],
                    awaitRefetchQueries: true
                })
                    .catch(error => {
                        dispatch({ type: 'couponError', error: parseError(error) });
                    })
                    .finally(() => {
                        dispatch({ type: 'endLoading' });
                    });
            },
            removeCoupon: () => {
                dispatch({ type: 'beginLoading' });
                return removeCoupon({
                    variables: { cartId },
                    refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }],
                    awaitRefetchQueries: true
                })
                    .catch(error => {
                        dispatch({ type: 'error', error: error.toString() });
                    })
                    .finally(() => {
                        dispatch({ type: 'endLoading' });
                    });
            }
        };
    };

    console.log(`Cart id from cookie is now ${cartId}, state is ${stateCartId}`);
    useEffect(() => {
        if (cartId && cartId.length > 0) {
            console.log(`Running the effect that puts the cart id ${cartId} in the state`);
            dispatch({ type: 'cartId', cartId, methods: createCartHandlers(cartId, dispatch) });
        } else if (stateCartId) {
            console.log(`Put the cart id in the cookie`);
            setCartCookie(stateCartId);
        }
    }, [cartId, stateCartId]);

    useEffect(() => {
        if (registeredCartId) {
            setCartCookie(registeredCartId);
            dispatch({ type: 'cartId', cartId: registeredCartId, methods: createCartHandlers(cartId, dispatch) });
        }
    }, [cartId]);

    return props.children;
};

export default CartInitializer;
