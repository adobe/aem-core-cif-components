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
import React, { useState, useEffect } from 'react';
import { func, shape, string, bool, object } from 'prop-types';
import { useQuery } from '@magento/peregrine';

import { useMutation } from '../../utils/useMutation';
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
    const [isOpen, setIsOpen] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [isEmpty, setIsEmpty] = useState(true);
    const [isLoading, setIsLoading] = useState(false);

    const [queryResult, queryApi] = useQuery(CART_DETAILS_QUERY);
    const { data, error, loading } = queryResult;
    const { runQuery, setLoading } = queryApi;

    const rootClass = isOpen ? classes.root_open : classes.root;

    useEffect(() => {
        console.log(`Running the query...`);
        setIsLoading(true), runQuery({ variables: { cartId: CART_ID } });

        if (error) {
            console.log(`Error loading cart`);
        } else {
            setCart({
                details: data && data.cart,
                currencyCode: data ? getCurrencyCode(data.cart) : ''
            });
            setIsEmpty(!data || !data.cart || data.cart.items.length === 0);
            setIsLoading(false);
        }
    }, [runQuery, setIsLoading, isOpen, setCart, setIsEmpty]);

    useEffect(() => {
        document.addEventListener('aem.cif.open-cart', event => {
            setIsOpen(true);
        });
    });

    const handleCloseCart = () => {
        setIsOpen(false);
    };

    const handleBeginEditing = () => {
        setIsEditing(true);
    };

    const [removeItemResult, removeItemApi] = useMutation(MUTATION_REMOVE_ITEM);
    const { runMutation } = removeItemApi;
    const removeItemFromCart = itemId => {
        runMutation({ variables: { cartId: CART_ID, itemId } });
        const { data, error, loading } = removeItemResult;

        console.log(`Do we have data after that? `, data);
        console.log(`Do we have errors? `, error);
    };

    return (
        <>
            <Mask isActive={isOpen} dismiss={handleCloseCart} />
            <aside className={rootClass}>
                <Header handleCloseCart={handleCloseCart} />
                <Body
                    isEmpty={isEmpty}
                    isEditing={isEditing}
                    isLoading={isLoading}
                    cart={cart.details}
                    currencyCode={cart.currencyCode}
                    removeItemFromCart={removeItemFromCart}
                />
                <Footer />
            </aside>
        </>
    );
};

export default MiniCart;
