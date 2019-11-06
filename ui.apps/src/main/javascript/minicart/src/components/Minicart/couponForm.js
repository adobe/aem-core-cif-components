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
import Button from '../Button';
import { useCartState } from './cartContext';

import classes from './couponForm.css';

const CouponForm = () => {
    const [{ addCoupon, couponError }] = useCartState();
    const [couponCode, setCouponCode] = useState('');

    const addCouponHandler = () => {
        return addCoupon(couponCode);
    };

    const errorFragment = couponError ? <div className={classes.error}>{couponError}</div> : '';

    return (
        <form className={classes.root}>
            <input
                value={couponCode}
                onChange={e => setCouponCode(e.target.value)}
                type="text"
                name="couponCode"
                placeholder="Enter your code"
            />
            <Button priority="high" onClick={addCouponHandler} disabled={couponCode.length < 3}>
                <span>Apply Coupon</span>
            </Button>
            {errorFragment}
        </form>
    );
};

export default CouponForm;
