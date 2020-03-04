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
import React, { useEffect } from 'react';
import { useQuery } from '@apollo/react-hooks';

import { useEventListener } from '../../utils/hooks';

import Mask from '../Mask';

import Header from './header';
import Body from './body';
import Footer from './footer';
import classes from './minicart.css';

import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import CartTrigger from '../CartTrigger';

import { useCartState } from './cartContext';
import { useUserContext } from '../../context/UserContext';

const MiniCart = () => {
    const [{ cartId, cart, isOpen, isLoading, isEditing, addItem, errorMessage }, dispatch] = useCartState();

    const { data, error, loading: queryLoading } = useQuery(CART_DETAILS_QUERY, {
        variables: { cartId },
        skip: !cartId
    });

    useEffect(() => {
        if (queryLoading) {
            dispatch({ type: 'beginLoading' });
        }
    }, [queryLoading]);

    useEffect(() => {
        if (data && data.cart) {
            dispatch({ type: 'cart', cart: data.cart });
        }
    }, [data]);

    if (error) {
        dispatch({ type: 'error', error: error.toString() });
    }

    useEventListener(document, 'aem.cif.open-cart', () => {
        dispatch({ type: 'open' });
    });
    useEventListener(document, 'aem.cif.add-to-cart', addItem);

    if (!cartId || cartId.length === 0) {
        return null;
    }

    const rootClass = isOpen ? classes.root_open : classes.root;
    const isEmpty = cart && Object.entries(cart).length > 0 ? cart.items.length === 0 : true;
    const showFooter = !(isLoading || isEmpty || isEditing || errorMessage);
    const footer = showFooter ? <Footer /> : null;

    return (
        <>
            <CartTrigger />
            <Mask />
            <aside className={rootClass}>
                <Header />
                <Body />
                {footer}
            </aside>
        </>
    );
};

export default MiniCart;
