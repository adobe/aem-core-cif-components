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
import { useCookieValue } from '../../utils/hooks';
import { useUserContext } from '../../context/UserContext';

const CartInitializer = props => {
    const [{ cartId: stateCartId }, dispatch] = useCartState();
    const [{ cartId: registeredCartId }] = useUserContext();

    const CART_COOKIE = 'cif.cart';

    const [cartId, setCartCookie] = useCookieValue(CART_COOKIE);

    useEffect(() => {
        if (cartId && cartId.length > 0 && !stateCartId) {
            dispatch({ type: 'cartId', cartId });
        }
    }, [cartId]);

    useEffect(() => {
        if (stateCartId && (!cartId || cartId.length === 0)) {
            setCartCookie(stateCartId);
        }
    }, [stateCartId]);

    useEffect(() => {
        if (registeredCartId) {
            setCartCookie(registeredCartId);
            dispatch({
                type: 'cartId',
                cartId: registeredCartId
            });
        }
    }, [registeredCartId]);

    return props.children;
};

export default CartInitializer;
