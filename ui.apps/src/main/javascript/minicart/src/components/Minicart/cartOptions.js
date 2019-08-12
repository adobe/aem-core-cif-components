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
import { Price } from '@magento/peregrine';

import classes from './cartOptions.css';

const CartOptions = props => {
    const { currencyCode, editItem } = props;

    const { product, quantity } = editItem;
    const { name, price: productPrice } = product;

    const { value, currency } = productPrice.regularPrice.amount;
    return (
        <div className={classes.root}>
            <div className={classes.focusItem}>
                <span className={classes.name}>{name}</span>
                <span className={classes.price}>
                    <Price currencyCode={currency} value={value} />
                </span>
            </div>
        </div>
    );
};

export default CartOptions;
