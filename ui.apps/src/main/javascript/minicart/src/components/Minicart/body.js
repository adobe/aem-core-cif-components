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
import React from 'react';
import { bool, string, func } from 'prop-types';

import LoadingIndicator from '../LoadingIndicator';

import EmptyMinicartBody from './emptyMinicartBody';
import classes from './body.css';
import ProductList from './productList';
import CartOptions from './cartOptions';

import { useCartState } from '../../utils/state';

const loadingIndicator = <LoadingIndicator>{`Fetching cart data...`}</LoadingIndicator>;

const Body = props => {
    const { isEmpty, isLoading, currencyCode, removeItemFromCart } = props;
    const [{ isEditing, cart }] = useCartState();

    if (isLoading) {
        return loadingIndicator;
    }

    if (isEmpty) {
        return <EmptyMinicartBody />;
    }
    if (isEditing) {
        return <CartOptions currencyCode={currencyCode} />;
    }

    const cartItems = cart.items;
    return (
        <div className={classes.root}>
            <ProductList cartItems={cartItems} currencyCode={currencyCode} removeItemFromCart={removeItemFromCart} />
        </div>
    );
};

export default Body;

Body.propTypes = {
    isEmpty: bool,
    isLoading: bool,
    currencyCode: string.isRequired,
    removeItemFromCart: func.isRequired
};
