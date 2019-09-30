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

import { useCartState } from '../../utils/state';

const MiniCart = () => {
    const [{ cartId, cart, isOpen, isLoading, isEditing, addItem }, dispatch] = useCartState();

    const { data, error, loading: queryLoading } = useQuery(CART_DETAILS_QUERY, {
        variables: { cartId },
        skip: !cartId
    });

    useEffect(() => {
        if (data && data.cart) {
            dispatch({ type: 'cart', cart: data.cart });
        }
    }, [data]);

    if (error) {
        console.error(`Error loading cart`, error);
    }

    useEventListener(document, 'aem.cif.open-cart', () => {
        dispatch({ type: 'open' });
    });
    useEventListener(document, 'aem.cif.add-to-cart', addItem);

    const rootClass = isOpen ? classes.root_open : classes.root;
    const isEmpty = data && data.cart && data.cart.items.length === 0;

    let cartQuantity;
    let footer = null;
    // TODO: Ideally this is stored in the cart state
    let loading = !data || !data.cart || queryLoading || isLoading;
    const showFooter = !(loading || isEmpty || isEditing);
    if (cart && Object.entries(cart).length > 0) {
        cartQuantity = cart.items.length;
        footer = showFooter ? <Footer /> : null;
    }

    if (!cartId || cartId.length === 0) {
        return null;
    }

    return (
        <>
            <CartTrigger cartQuantity={cartQuantity} />
            <Mask />
            <aside className={rootClass}>
                <Header />
                <Body isEmpty={isEmpty} isLoading={loading} />
                {footer}
            </aside>
        </>
    );
};

export default MiniCart;
