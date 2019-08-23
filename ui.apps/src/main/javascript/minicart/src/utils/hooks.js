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

export const useGuestCart = () => {
    let cookieName = 'cif.cart';
    const getInitialCartId = () => {
        if (checkCookie(cookieName)) {
            const cifCartCookie = cookieValue(cookieName);
            return cifCartCookie;
        } else {
            return '';
        }
    };

    let initialCartId = getInitialCartId();
    if (initialCartId) {
        return initialCartId;
    }

    const [cartId, setCartId] = useState('');
    const [createCart, { data, loading }] = useMutation(MUTATION_CREATE_CART);

    useEffect(() => {
        if (!cartId || cartId.length === 0) {
            createCart();

            if (data) {
                setCartId(data.createEmptyCart);
                document.cookie = `${cookieName}=${data.createEmptyCart};path=/`;
            }
        }
    }, [loading, document]);

    return cartId;
};

export const useCountries = () => {
    const { data, loading, error } = useQuery(QUERY_COUNTRIES);
    const [countries, setCountries] = useState([]);
    useEffect(() => {
        console.log(`Is it loading?`, loading, data);
        if (error) {
            throw new Error(error);
        }
        if (data) {
            setCountries(data.countries);
        }
    }, [loading]);

    return countries;
};
