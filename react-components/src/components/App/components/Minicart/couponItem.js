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
import { useCartState } from './cartContext';
import Kebab from './kebab';
import Section from './section';

import classes from './couponItem.css';

const CouponItem = () => {
    const [{ cart, removeCoupon }] = useCartState();

    const appliedCoupon = cart.applied_coupon ? cart.applied_coupon.code : null;

    return (
        <div className={classes.root}>
            <div className={classes.couponName}>
                Coupon <strong>{appliedCoupon}</strong> applied.
            </div>
            <Kebab>
                <Section text="Remove coupon" onClick={removeCoupon} icon="Trash" />
            </Kebab>
        </div>
    );
};

export default CouponItem;
