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
import classes from './minicart.css';

import Header from './header';
import Body from './body';
import Footer from './footer';
import Mask from '../Mask';

const MiniCart = props => {
    const { isOpen, handleCloseCart, cart } = props;

    const { isLoading, details, isEmpty, currencyCode } = cart;
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
                />
                <Footer />
            </aside>
        </>
    );
};

export default MiniCart;
