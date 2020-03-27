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
/**
 * Creates an empty cart
 *
 * @param {Object} payload - the dispatch function, the create cart mutation, the cart state
 */
import { notUseCookieValue } from '../../utils/hooks';

export const createCart = async payload => {
    const { dispatch, createCartMutation } = payload;

    dispatch({ type: 'beginLoading' });

    const [cartCookieValue, setCartCookie] = notUseCookieValue('cif.cart');

    if (cartCookieValue) {
        dispatch({ type: 'cartId', cartId: cartCookieValue });
        return;
    }

    try {
        const { data, errors } = await createCartMutation();
        const cartId = data.createEmptyCart;
    } catch (error) {
        dispatch({ type: error, error });
    }
};
