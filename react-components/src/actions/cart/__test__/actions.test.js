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
import { addItemToCart, getCartDetails } from '../actions';
describe('Cart actions', () => {
    const addToCartMutation = jest.fn();
    const cartDetailsQuery = jest.fn(() => {
        return { data: { cart: { id: 'guest123' } } };
    });
    const createCartMutation = jest.fn(() => {
        return { data: { createEmptyCart: 'guest123' } };
    });
    const dispatch = jest.fn();

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('handles the "addToCart" action and updates state', async () => {
        const items = [];
        const cart = { cartId: 'guest123' };

        await addItemToCart({
            createCartMutation,
            addToCartMutation,
            cartDetailsQuery,
            dispatch,
            items,
            cartId: cart.cartId
        });

        expect(createCartMutation).toHaveBeenCalledTimes(0);
        expect(addToCartMutation).toHaveBeenCalledTimes(1);
        expect(cartDetailsQuery).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'open' });
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: 'guest123' } });
    });

    it('creates the cart if none is available in the state', async () => {
        const addItemMutation = jest.fn();
        const cartDetailsQuery = jest.fn();
        const createCartMutation = jest.fn(() => {
            return { data: { createEmptyCart: 'guest123' } };
        });
        const dispatch = jest.fn();
        const items = [];
        const cart = {};

        await addItemToCart({ createCartMutation, addItemMutation, cartDetailsQuery, dispatch, items, cart });

        expect(createCartMutation).toHaveBeenCalledTimes(1);
    });

    it('retrieves the cart details and updates state', async () => {
        const cartId = 'guest123';
        await getCartDetails({ cartDetailsQuery, dispatch, cartId });

        expect(cartDetailsQuery).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'cart', cart: { id: 'guest123' } });
    });
});
