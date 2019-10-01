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
import CART_DETAILS_QUERY from '../queries/query_cart_details.graphql';
import MUTATION_REMOVE_ITEM from '../queries/mutation_remove_item.graphql';
import MUTATION_ADD_TO_CART from '../queries/mutation_add_to_cart.graphql';

const CartInitializer = props => {
    const [{ cartId: stateCartId }, dispatch] = useCartState();
    const CART_COOKIE = 'cif.cart';

    const [cartId, setCartCookie] = useCookieValue(CART_COOKIE);
    const [createCart, { data, error }] = useMutation(MUTATION_CREATE_CART);
    const [addItem] = useMutation(MUTATION_ADD_TO_CART);
    const [removeItem] = useMutation(MUTATION_REMOVE_ITEM);

    const createCartHandlers = (cartId, dispatch) => {
        return {
            addItem: ev => {
                if (!ev.detail) return;

                const { sku, quantity } = ev.detail;
                dispatch({ type: 'open' });
                dispatch({ type: 'beginLoading' });
                return addItem({
                    variables: { cartId, sku, quantity },
                    refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }],
                    awaitRefetchQueries: true
                })
                    .catch(error => {
                        dispatch({ type: 'error', error: error });
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
                        dispatch({ type: 'error', error: error });
                    })
                    .finally(() => {
                        dispatch({ type: 'endLoading' });
                    });
            }
        };
    };

    useEffect(() => {
        if (!cartId || cartId.length === 0) {
            createCart();
        }
    }, [cartId]);

    useEffect(() => {
        if (cartId && (!stateCartId || stateCartId.length === 0)) {
            dispatch({ type: 'cartId', cartId: cartId, methods: createCartHandlers(cartId, dispatch) });
        }
    }, [cartId, stateCartId]);

    useEffect(() => {
        if (data) {
            setCartCookie(data.createEmptyCart);
            dispatch({
                type: 'cartId',
                cartId: data.createEmptyCart,
                methods: createCartHandlers(data.createEmptyCart, dispatch)
            });
        }

        if (error) {
            dispatch({ type: 'error', error: error });
        }
    }, [data, error]);

    return props.children;
};

export default CartInitializer;
