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
import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@apollo/react-hooks';
import { checkCookie, cookieValue } from './cookieUtils';

import MUTATION_CREATE_CART from '../queries/mutation_create_guest_cart.graphql';
import QUERY_COUNTRIES from '../queries/query_countries.graphql';

export const useEventListener = (target, type, listener, ...rest) => {
    useEffect(() => {
        target.addEventListener(type, listener, ...rest);

        // return a callback, which is called on unmount
        return () => {
            target.removeEventListener(type, listener, ...rest);
        };
    }, [listener, rest, target, type]);
};

export const useCookieValue = cookieName => {
    if (!cookieName || cookieName.length === 0) {
        return '';
    }
    let value = checkCookie(cookieName) ? cookieValue(cookieName) : '';
    const setCookieValue = (value, age) => {
        const cookieSettings = `path=/; domain=${window.location.host};Max-Age=${age !== undefined ? age : 3600}`;
        document.cookie = `${cookieName}=${value};${cookieSettings}`;
    };

    return [value, setCookieValue];
};

export const useGuestCart = () => {
    const cookieName = 'cif.cart';
    const [reset, doReset] = useState(false);
    let [cookieCartId, setCartCookie] = useCookieValue(cookieName);
    const [cartId, setCartId] = useState(cookieCartId);

    const [createCart, { data }] = useMutation(MUTATION_CREATE_CART);

    useEffect(() => {
        if (!cartId || cartId.length === 0) {
            createCart();
        }
    }, [cartId]);

    useEffect(() => {
        if (data) {
            setCartId(data.createEmptyCart);
            setCartCookie(data.createEmptyCart);
        }
    }, [data]);

    const resetGuestCart = () => {
        setCartCookie('', 0);
        doReset(!reset);
    };

    return [cartId, resetGuestCart];
};

export const useCountries = () => {
    const { data } = useQuery(QUERY_COUNTRIES);
    if (!data && !data.countries) {
        return [];
    }

    return data.countries;
};
