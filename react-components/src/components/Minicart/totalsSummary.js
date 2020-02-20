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
import { number, string, shape } from 'prop-types';

import { Price } from '@magento/peregrine';

import classes from './totalsSummary.css';

const TotalsSummary = props => {
    // Props.
    const { numItems, subtotal, subtotalDiscount } = props;

    // Do not display price, if cart is empty. But display price, if the cart has
    // items and the price is 0 (e.g. when coupons are applied).
    const hasSubtotal = Boolean(subtotal.value) || numItems > 0;
    const hasDiscount = subtotal.value !== subtotalDiscount.value;
    const numItemsText = numItems === 1 ? 'item' : 'items';

    let priceClasses = {};
    if (hasDiscount) {
        priceClasses = {
            currency: classes.discounted,
            integer: classes.discounted,
            decimal: classes.discounted,
            fraction: classes.discounted
        };
    }

    return (
        <div className={classes.root}>
            {hasSubtotal && (
                <dl className={classes.totals}>
                    <dt className={classes.subtotalLabel}>
                        <span>
                            {'Cart Total: '}
                            <Price classes={priceClasses} currencyCode={subtotal.currency} value={subtotal.value} />
                        </span>
                    </dt>
                    <dd className={classes.subtotalValue}>
                        ({numItems} {numItemsText})
                    </dd>
                </dl>
            )}
            {hasSubtotal && hasDiscount && (
                <dl className={classes.totalsDiscount}>
                    <dt className={classes.subtotalLabel}>
                        <span>
                            {'New Cart Total: '}
                            <Price currencyCode={subtotalDiscount.currency} value={subtotalDiscount.value} />
                        </span>
                    </dt>
                    <dd className={classes.subtotalValue}>
                        ({numItems} {numItemsText})
                    </dd>
                </dl>
            )}
        </div>
    );
};

TotalsSummary.propTypes = {
    currencyCode: string,
    numItems: number,
    subtotal: shape({
        currency: string,
        value: number
    }),
    subtotalDiscount: shape({
        currency: string,
        value: number
    })
};

export default TotalsSummary;
