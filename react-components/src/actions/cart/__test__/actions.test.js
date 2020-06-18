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
import {
    addItemToCart,
    getCartDetails,
    removeItemFromCart,
    mergeCarts,
    addCoupon,
    removeCoupon,
    updateCartItem
} from '../actions';

describe('Cart actions', () => {
    const addToCartMutation = jest.fn();

    const cartDetailsQuery = jest.fn(args => {
        const cartId = args ? args.variables.cartId : 'guest123';
        return { data: { cart: { id: cartId } } };
    });

    const createCartMutation = jest.fn(() => {
        return { data: { createEmptyCart: 'guest123' } };
    });

    const mergeCartsMutation = jest.fn(args => {
        return { data: { mergeCarts: { id: args.variables.destinationCartId } } };
    });

    const addCouponMutation = jest.fn();
    const removeCouponMutation = jest.fn();
    const removeItemMutation = jest.fn();

    const updateCartItemMutation = jest.fn();

    const dispatch = jest.fn();

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('handles the "addToCart" action and updates state', async () => {
        const physicalCartItems = [];
        const virtualCartItems = [];
        const cart = { cartId: 'guest123' };

        await addItemToCart({
            createCartMutation,
            addToCartMutation,
            cartDetailsQuery,
            dispatch,
            physicalCartItems,
            virtualCartItems,
            cartId: cart.cartId
        });

        expect(createCartMutation).toHaveBeenCalledTimes(0);
        expect(addToCartMutation).toHaveBeenCalledTimes(1);
        expect(cartDetailsQuery).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: 'guest123' } });
    });

    it('creates the cart if none is available in the state', async () => {
        const addItemMutation = jest.fn();
        const cartDetailsQuery = jest.fn();
        const createCartMutation = jest.fn(() => {
            return { data: { createEmptyCart: 'guest123' } };
        });
        const dispatch = jest.fn();
        const physicalCartItems = [];
        const virtualCartItems = [];
        const cart = {};

        await addItemToCart({
            createCartMutation,
            addItemMutation,
            cartDetailsQuery,
            dispatch,
            physicalCartItems,
            virtualCartItems,
            cart
        });

        expect(createCartMutation).toHaveBeenCalledTimes(1);
    });

    it('removes an item from the cart', async () => {
        const cartId = 'guest123';
        const itemId = 'doesntmatter';

        await removeItemFromCart({ cartDetailsQuery, removeItemMutation, cartId, itemId, dispatch });
        expect(removeItemMutation).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: 'guest123' } });
    });

    it('retrieves the cart details and updates state', async () => {
        const cartId = 'guest123';
        await getCartDetails({ cartDetailsQuery, dispatch, cartId });

        expect(cartDetailsQuery).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: 'guest123' } });
    });

    it('executes the "merge carts" action', async () => {
        const cartId = 'guest123';
        const customerCartId = 'notguest123';

        await mergeCarts({ cartDetailsQuery, mergeCartsMutation, cartId, customerCartId, dispatch });
        expect(mergeCartsMutation).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: customerCartId } });
    });

    it('adds a coupon to a cart', async () => {
        const cartId = 'guest123';
        const couponCode = '10off';

        await addCoupon({ cartDetailsQuery, addCouponMutation, couponCode, cartId, dispatch });

        expect(addCouponMutation).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: cartId } });
    });

    it('removes a coupon from the cart', async () => {
        const cartId = 'guest123';
        const couponCode = '10off';

        await removeCoupon({ cartDetailsQuery, removeCouponMutation, couponCode, cartId, dispatch });

        expect(removeCouponMutation).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: cartId } });
    });

    it('update quantity in the cart', async () => {
        const cartId = 'guest123';
        const cartItemId = '1';
        const itemQuantity = 2;

        await updateCartItem({ cartDetailsQuery, updateCartItemMutation, cartId, cartItemId, itemQuantity, dispatch });
        expect(updateCartItemMutation).toHaveBeenCalledTimes(1);
        expect(updateCartItemMutation).toHaveBeenCalledWith({
            variables: { cartId, cartItemId, quantity: itemQuantity }
        });

        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: cartId } });
    });
});
