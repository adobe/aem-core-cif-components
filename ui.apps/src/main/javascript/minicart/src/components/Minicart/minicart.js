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
import { func, shape, string, bool, object } from 'prop-types';
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

//TODO retrieve this from the cookie.
const CART_ID = 'hx7geWblhhU0znC4rFPR166UvNy2Mp1k';

const MiniCart = props => {
    const [cart, setCart] = useState({
        details: undefined,
        currencyCode: ''
    });
    const [isOpen, setIsOpen] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [isEmpty, setIsEmpty] = useState(true);

    const [removeItem] = useMutation(MUTATION_REMOVE_ITEM, {
        refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId: CART_ID } }]
    });

    const { data, error, loading } = useQuery(CART_DETAILS_QUERY, { variables: { cartId: CART_ID } });
    const rootClass = isOpen ? classes.root_open : classes.root;

    if (error) {
        console.log(`Error loading cart`, error);
    }

    const openCart = () => {
        setIsOpen(true);
    };

    useEventListener(document, 'aem.cif.open-cart', openCart);

    const handleCloseCart = () => {
        setIsOpen(false);
    };

    const handleBeginEditing = () => {
        setIsEditing(true);
    };

    const removeItemFromCart = itemId => {
        removeItem({ variables: { cartId: CART_ID, itemId } });
    };

    return (
        <>
            <Mask isActive={isOpen} dismiss={handleCloseCart} />
            <aside className={rootClass}>
                <Header handleCloseCart={handleCloseCart} />
                <Body
                    isEmpty={data && data.cart && data.cart.items.length === 0}
                    isEditing={isEditing}
                    isLoading={loading}
                    cart={data.cart}
                    currencyCode={cart.currencyCode}
                    removeItemFromCart={removeItemFromCart}
                />
                <Footer />
            </aside>
        </>
    );
};

export default MiniCart;
