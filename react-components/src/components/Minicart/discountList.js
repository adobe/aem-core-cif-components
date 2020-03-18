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
import { number, shape, arrayOf, string } from 'prop-types';

import Price from '../Price';

import classes from './discountList.css';

const DiscountList = props => {
    let _renderDiscounts = () => {
        return props.discounts.map(discount => {
            const { currency, value } = discount.amount;
            return (
                <div className={classes.item} key={discount.label}>
                    <span>{discount.label}</span>
                    <span className={classes.price}>
                        <Price currencyCode={currency} value={-value} />
                    </span>
                </div>
            );
        });
    };

    return <div className={classes.root}>{_renderDiscounts()}</div>;
};

DiscountList.propTypes = {
    discounts: arrayOf(
        shape({
            amount: shape({
                value: number.isRequired,
                currency: string.isRequired
            }).isRequired,
            label: string.isRequired
        })
    )
};

export default DiscountList;
