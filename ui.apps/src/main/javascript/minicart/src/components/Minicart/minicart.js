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
import React, { useCallback } from 'react';
import { string, func } from 'prop-types';
import { useQuery, useMutation } from '@apollo/react-hooks';

import { useEventListener } from '../../utils/hooks';
import getCurrencyCode from '../../utils/getCurrencyCode';

import Mask from '../Mask';

import Header from './header';
import Body from './body';
import Footer from './footer';
import classes from './minicart.css';

import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import MUTATION_REMOVE_ITEM from '../../queries/mutation_remove_item.graphql';
import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';
import CartTrigger from '../CartTrigger';

import { useCartState } from '../../utils/state';

const MiniCart = props => {
    const { cartId, resetCart } = props;

    const [{ isOpen, isEditing }, dispatch] = useCartState();

    const [addItem, { loading: addItemLoading }] = useMutation(MUTATION_ADD_TO_CART, {
        refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }]
    });
    const [removeItem, { loading: removeItemLoading }] = useMutation(MUTATION_REMOVE_ITEM, {
        refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }]
    });

    const { data, error, loading: queryLoading } = useQuery(CART_DETAILS_QUERY, { variables: { cartId } });

    if (error) {
        console.error(`Error loading cart`, error);
    }

    const addToCart = ev => {
        if (!ev.detail) {
            return;
        }

        const { sku, quantity } = ev.detail;
        addItem({ variables: { cartId, sku, quantity } });
        dispatch({ type: 'open' });
    };

    useEventListener(document, 'aem.cif.open-cart', () => {
        dispatch({ type: 'open' });
    });
    useEventListener(document, 'aem.cif.add-to-cart', addToCart);

    const handleResetCart = useCallback(() => {
        resetCart();
        dispatch({ type: 'close' });
    });

    const removeItemFromCart = useCallback(
        itemId => {
            removeItem({ variables: { cartId, itemId } });
        },
        [removeItem]
    );

    const rootClass = isOpen ? classes.root_open : classes.root;
    const isEmpty = data && data.cart && data.cart.items.length === 0;

    let cartQuantity;
    let currencyCode = '';
    let footer = null;
    const isLoading = !data || !data.cart || queryLoading || addItemLoading || removeItemLoading;
    const showFooter = !(isLoading || isEmpty || isEditing);
    if (data && data.cart) {
        currencyCode = getCurrencyCode(data.cart);
        cartQuantity = data.cart.items.length;
        footer = showFooter ? <Footer cart={data.cart} cartId={cartId} handleResetCart={handleResetCart} /> : null;
    }

    return (
        <>
            <CartTrigger cartQuantity={cartQuantity} />
            <Mask />
            <aside className={rootClass}>
                <Header />
                <Body
                    isEmpty={isEmpty}
                    isLoading={isLoading}
                    cart={data && data.cart}
                    currencyCode={currencyCode}
                    removeItemFromCart={removeItemFromCart}
                    cartId={cartId}
                />
                {footer}
            </aside>
        </>
    );
};

MiniCart.propTypes = {
    cartId: string.isRequired,
    resetCart: func.isRequired
};

export default MiniCart;
