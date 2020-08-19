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
 * Adds an item to the cart. If the cart doesn't exist then it's created
 *
 * @param {Object} payload a parameters object with the following structure:
 *      cartDetailsQuery - the query object to execute
 *      createCartMutation - the mutation to execute to create the cart
 *      addToCartMutation - the mutation to execute to add the item to the cart
 *      dispatch - the dispatch callback for the cart context
 *      cartItems - the items to add to the cart
 *      cartId - the id of the cart in which to add the items. This could be `undefined` or `null`
 */

export const addItemToCart = async payload => {
    const {
        createCartMutation,
        cartDetailsQuery,
        addToCartMutation,
        dispatch,
        physicalCartItems,
        virtualCartItems
    } = payload;

    try {
        let cartId = payload.cartId;
        if (!payload.cartId) {
            let { data: newCartData } = await createCartMutation();
            cartId = newCartData.createEmptyCart;
        }

        let variables = { cartId, cartItems: physicalCartItems };
        if (physicalCartItems.length > 0 && virtualCartItems.length > 0) {
            variables = { cartId, virtualCartItems, simpleCartItems: physicalCartItems };
        } else if (virtualCartItems.length > 0) {
            variables = { cartId, cartItems: virtualCartItems };
        }

        await addToCartMutation({ variables });
        dispatch({ type: 'cartId', cartId });
        await getCartDetails({ cartDetailsQuery, cartId, dispatch });
    } catch (error) {
        dispatch({ type: 'error', error: error.toString() });
    }
};

/**
 * Retrieves the cart details
 * @param {Object} payload a parameters object with the following structure:
 *      cartDetailsQuery - the query object to execute
 *      dispatch - the dispatch callback for the cart context
 *      cartId - the id of the cart
 */
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

/**
 * Removes an item from the cart
 *
 * @param {Object} payload a parameters object with the following structure:
 *      cartDetailsQuery - the query object to execute to retrieve the details
 *      removeItemMutation - the mutation to execute to remove the items
 *      dispatch - the dispatch callback for the cart context
 *      cartId - the id of the cart
 *      itemId - the id of the item to remove
 */
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

/**
 * Removes a coupon from the cart
 *
 * @param {Object} payload a parameters object with the following structure:
 *      cartDetailsQuery - the query object to execute
 *      removeCouponMutation - the mutation to execute to remove the coupon
 *      dispatch - the dispatch callback for the cart context
 *      cartId - the id of the cart
 *      couponCode - the code of the coupon to be removed
 */
export const removeCoupon = async payload => {
    const { cartDetailsQuery, cartId, couponCode, removeCouponMutation, dispatch } = payload;

    try {
        await removeCouponMutation({ variables: { cartId, couponCode } });
    } catch (error) {
        dispatch({ type: 'couponError', error: parseError(error) });
    }

    await getCartDetails({ cartDetailsQuery, dispatch, cartId });
};

/**
 * Adds a coupon to the cart
 *
 * @param {Object} payload a parameters object with the following structure:
 *      cartDetailsQuery - the query object to execute
 *      addCouponMutation - the mutation to execute to add the coupon to the cart
 *      dispatch - the dispatch callback for the cart context
 *      cartId - the id of the cart
 *      couponCode - the code of the coupon
 */
export const addCoupon = async payload => {
    const { cartDetailsQuery, cartId, couponCode, addCouponMutation, dispatch } = payload;

    try {
        await addCouponMutation({ variables: { cartId, couponCode } });
    } catch (error) {
        dispatch({ type: 'couponError', error: parseError(error) });
    }

    await getCartDetails({ cartDetailsQuery, dispatch, cartId });
};

/**
 * Merges two shopping carts
 *
 * @param {Object} payload a parameters object with the following structure:
 *      cartDetailsQuery - the query object to execute
 *      mergeCartsMutation - the mutation to execute to merge the carts
 *      dispatch - the dispatch callback for the cart context
 *      cartId - the id of the cart to be merged
 *      customerCartId - the id of the cart that the other cart will the merged into (i.e. the customer cart)
 */
export const mergeCarts = async payload => {
    const { cartDetailsQuery, mergeCartsMutation, cartId, customerCartId, dispatch } = payload;
    try {
        const { data } = await mergeCartsMutation({
            variables: {
                sourceCartId: cartId,
                destinationCartId: customerCartId
            }
        });
        await getCartDetails({ cartDetailsQuery, dispatch, cartId: data.mergeCarts.id });
        return data.mergeCarts.id;
    } catch (error) {
        dispatch({ type: 'error', error: parseError(error) });
    }
};

export const updateCartItem = async payload => {
    const { cartDetailsQuery, updateCartItemMutation, cartId, cartItemId, itemQuantity, dispatch } = payload;
    try {
        await updateCartItemMutation({
            variables: { cartId, cartItemId, quantity: itemQuantity }
        });
        await getCartDetails({ cartDetailsQuery, dispatch, cartId });
    } catch (error) {
        dispatch({ type: 'error', error: parseError(error) });
    }
};
