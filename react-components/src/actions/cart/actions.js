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
import parseError from '../../utils/parseError';
/**
 * Adds an item to the cart
 * @param {Object} payload
 */

export const addItemToCart = async payload => {
    const { createCartMutation, cartDetailsQuery, addToCartMutation, dispatch, cartItems } = payload;

    dispatch({ type: 'open' });
    dispatch({ type: 'beginLoading' });

    try {
        let cartId = payload.cartId;
        if (!payload.cartId) {
            let { data: newCartData } = await createCartMutation();
            cartId = newCartData.createEmptyCart;
        }

        await addToCartMutation({
            variables: { cartId, cartItems }
        });
        dispatch({ type: 'cartId', cartId });
        await getCartDetails({ cartDetailsQuery, cartId, dispatch });
    } catch (error) {
        dispatch({ type: 'error', error: error.toString() });
    } finally {
        dispatch({ type: 'endLoading' });
    }
};

export const getCartDetails = async payload => {
    const { cartDetailsQuery, dispatch, cartId } = payload;

    try {
        if (!cartId) {
            return;
        }
        const { data, error } = await cartDetailsQuery({ variables: { cartId }, fetchPolicy: 'network-only' });
        if (error) {
            throw new Error(error);
        }

        dispatch({ type: 'cart', cart: data.cart });
    } catch (error) {
        dispatch({ type: 'error', error: error.toString() });
    }
};

export const removeItemFromCart = async payload => {
    const { cartDetailsQuery, removeItemMutation, cartId, itemId, dispatch } = payload;

    try {
        await removeItemMutation({
            variables: { cartId, itemId }
        });
    } catch (error) {
        dispatch({ type: 'error', error: error.toString() });
    }
    await getCartDetails({ cartDetailsQuery, dispatch, cartId });
};

export const removeCoupon = async payload => {
    const { cartDetailsQuery, cartId, couponCode, removeCouponMutation, dispatch } = payload;

    try {
        await removeCouponMutation({ variables: { cartId, couponCode } });
    } catch (error) {
        dispatch({ type: 'couponError', error: parseError(error) });
    }

    await getCartDetails({ cartDetailsQuery, dispatch, cartId });
};
export const addCoupon = async payload => {
    const { cartDetailsQuery, cartId, couponCode, addCouponMutation, dispatch } = payload;

    try {
        await addCouponMutation({ variables: { cartId, couponCode } });
    } catch (error) {
        dispatch({ type: 'couponError', error: parseError(error) });
    }

    await getCartDetails({ cartDetailsQuery, dispatch, cartId });
};
