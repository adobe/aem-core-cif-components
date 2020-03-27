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
import { useMutation, useApolloClient } from '@apollo/react-hooks';
import { useUserContext } from '../../context/UserContext';

import parseError from '../../utils/parseError';

import MUTATION_CREATE_CART from '../../queries/mutation_create_guest_cart.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import MUTATION_REMOVE_ITEM from '../../queries/mutation_remove_item.graphql';
import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';
import MUTATION_ADD_COUPON from '../../queries/mutation_add_coupon.graphql';
import MUTATION_REMOVE_COUPON from '../../queries/mutation_remove_coupon.graphql';

const CartInitializer = props => {
    const [{ cartId: stateCartId }, dispatch] = useCartState();
    const [{ cartId: registeredCartId }] = useUserContext();

    const CART_COOKIE = 'cif.cart';

    const [cartId, setCartCookie] = useCookieValue(CART_COOKIE);

    const [createCart] = useMutation(MUTATION_CREATE_CART);
    const [addItem] = useMutation(MUTATION_ADD_TO_CART);
    const [removeItem] = useMutation(MUTATION_REMOVE_ITEM);
    const [addCoupon] = useMutation(MUTATION_ADD_COUPON);
    const [removeCoupon] = useMutation(MUTATION_REMOVE_COUPON);

    const createCartHandlers = (cartId, dispatch) => {
        return {
            addItem: ev => {
                if (!ev.detail) return;

                let cartItems = ev.detail.map(item => {
                    return {
                        data: {
                            sku: item.sku,
                            quantity: item.quantity
                        }
                    };
                });
                dispatch({ type: 'open' });
                dispatch({ type: 'beginLoading' });
                return addItem({
                    variables: { cartId, cartItems },
                    refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }],
                    awaitRefetchQueries: true
                })
                    .catch(error => {
                        dispatch({ type: 'error', error: error.toString() });
                    })
                    .finally(() => {
                        dispatch({ type: 'endLoading' });
                    });
            },
            removeItem: itemId => {
                dispatch({ type: 'beginLoading' });
                return removeItem({
                    variables: { cartId, itemId },
                    refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }],
                    awaitRefetchQueries: true
                })
                    .catch(error => {
                        dispatch({ type: 'error', error: error.toString() });
                    })
                    .finally(() => {
                        dispatch({ type: 'endLoading' });
                    });
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
        }
    }, [cartId]);

    useEffect(() => {
        if (!registeredCartId && (cartId === null || cartId.length === 0)) {
            console.log(`Running the effect with the cart id`);
            (async function() {
                const { data } = await createCart();
                console.log(`Created empty cart ${data.createEmptyCart}`);
                setCartCookie(data.createEmptyCart);
                dispatch({
                    type: 'cartId',
                    cartId: data.createEmptyCart,
                    methods: createCartHandlers(data.createEmptyCart, dispatch)
                });
            })();
        }

        if (registeredCartId) {
            setCartCookie(registeredCartId);
            dispatch({ type: 'cartId', cartId: registeredCartId, methods: createCartHandlers(cartId, dispatch) });
        }
    }, [cartId]);

    return props.children;
};

export default CartInitializer;
