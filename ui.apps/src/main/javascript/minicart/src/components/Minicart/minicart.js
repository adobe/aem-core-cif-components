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
import React, { useState } from 'react';
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

//TODO retrieve this from the cookie.
const CART_ID = 'V1bvif5UxQThb84iukrxHx9dYQg9nr8j';

const MiniCart = props => {
    console.log(`Rendering the minicart`);
    const [isOpen, setIsOpen] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [editItem, setEditItem] = useState({});
    const [cartId, setCartId] = useState(CART_ID);

    const [addItem] = useMutation(MUTATION_ADD_TO_CART, {
        refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }]
    });
    const [removeItem] = useMutation(MUTATION_REMOVE_ITEM, {
        refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }]
    });

    const { data, error, loading } = useQuery(CART_DETAILS_QUERY, { variables: { cartId } });

    if (error) {
        console.log(`Error loading cart`, error);
    }

    const openCart = () => {
        setIsOpen(true);
    };

    const addToCart = ev => {
        if (!ev.detail) {
            return;
        }

        const { sku, quantity } = ev.detail;
        addItem({ variables: { cartId, sku, quantity } });
        setIsOpen(true);
    };

    useEventListener(document, 'aem.cif.open-cart', openCart);
    useEventListener(document, 'aem.cif.add-to-cart', addToCart);

    const handleCloseCart = () => {
        setIsOpen(false);
    };

    const handleBeginEditing = item => {
        setIsEditing(true);
        setEditItem(item);
    };

    const handleEndEditing = () => {
        setIsEditing(false);
    };

    const removeItemFromCart = itemId => {
        removeItem({ variables: { cartId, itemId } });
    };

    const rootClass = isOpen ? classes.root_open : classes.root;
    const isEmpty = data && data.cart && data.cart.items.length === 0;
    console.log(`Is the mini-cart loading? ${loading}`);
    const currencyCode = getCurrencyCode(data.cart);
    let cartQuantity;

    if (data && data.cart) {
        cartQuantity = data.cart.items.length;
    }

    return (
        <>
            <CartTrigger cartQuantity={cartQuantity} handler={setIsOpen} />
            <Mask isActive={isOpen} dismiss={handleCloseCart} />
            <aside className={rootClass}>
                <Header handleCloseCart={handleCloseCart} />
                <Body
                    editItem={editItem}
                    isEmpty={isEmpty}
                    isEditing={isEditing}
                    isLoading={loading}
                    cart={data.cart}
                    currencyCode={currencyCode}
                    removeItemFromCart={removeItemFromCart}
                    beginEditItem={handleBeginEditing}
                    handleEndEditing={handleEndEditing}
                    cartId={cartId}
                />
                {loading || isEmpty || isEditing || <Footer isOpen={isOpen} cart={data.cart} />}
            </aside>
        </>
    );
};

export default MiniCart;
