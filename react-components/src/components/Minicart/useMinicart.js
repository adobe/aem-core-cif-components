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
import { addItemToCart, getCartDetails } from '../../actions/cart';
import { useCartState } from '../Minicart/cartContext';

export default ({ queries }) => {
    const { createCartMutation, addToCartMutation, cartDetailsQuery, addVirtualItemMutation } = queries;

    const [{ cartId, cart, isOpen, isLoading, isEditing, errorMessage }, dispatch] = useCartState();
    useEffect(() => {
        async function fn() {
            await getCartDetails({ cartDetailsQuery, dispatch, cartId });
        }

        fn();
    }, [cartId]);

    const addItem = async event => {
        if (!event.detail) return;

        let cartItems = event.detail.items.map(item => {
            return {
                data: {
                    sku: item.sku,
                    quantity: item.quantity
                }
            };
        });

        let addItemFn = event.detail.virtual ? addVirtualItemMutation : addToCartMutation;

        dispatch({ type: 'open' });
        dispatch({ type: 'beginLoading' });
        await addItemToCart({
            createCartMutation,
            addToCartMutation: addItemFn,
            cartDetailsQuery,
            cart,
            cartId,
            dispatch,
            cartItems
        });
        dispatch({ type: 'endLoading' });
    };

    const data = { cartId, cart, isOpen, isLoading, isEditing, errorMessage };
    const api = { addItem, dispatch };

    return [data, api];
};
