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
import { func, shape, string, bool, object } from 'prop-types';

import Mask from '../Mask';

import Header from './header';
import Body from './body';
import Footer from './footer';
import classes from './minicart.css';

const MiniCart = props => {
    const { isOpen, handleCloseCart, removeItemFromCart, cart } = props;

    const { isLoading, details, isEmpty, currencyCode, cartId } = cart;
    const isEditing = false;
    const rootClass = isOpen ? classes.root_open : classes.root;

    return (
        <>
            <Mask isActive={isOpen} dismiss={handleCloseCart} />
            <aside className={rootClass}>
                <Header handleCloseCart={handleCloseCart} />
                <Body
                    isEmpty={isEmpty}
                    isEditing={isEditing}
                    isLoading={isLoading}
                    cart={details}
                    currencyCode={currencyCode}
                    removeItemFromCart={removeItemFromCart}
                />
                <Footer />
            </aside>
        </>
    );
};

MiniCart.propTypes = {
    isOpen: bool.isRequired,
    handleCloseCart: func,
    handleRemoveItemFromCart: func,
    cart: shape({
        details: object,
        cartId: string,
        currencyCode: string.isRequired,
        isEmpty: bool.isRequired,
        isLoading: bool
    })
};

export default MiniCart;
