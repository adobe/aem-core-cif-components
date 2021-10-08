/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import Button from '../Button';

import classes from './couponForm.css';
import useCouponForm from './useCouponForm';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const CouponForm = () => {
    const [{ couponError }, { addCouponToCart }] = useCouponForm();
    const [couponCode, setCouponCode] = useState('');
    const intl = useIntl();

    const addCouponHandler = () => {
        return addCouponToCart(couponCode);
    };

    const errorFragment = couponError ? <div className={classes.error}>{couponError}</div> : '';

    return (
        <form className={classes.root}>
            <input
                value={couponCode}
                onChange={e => setCouponCode(e.target.value)}
                type="text"
                name="couponCode"
                placeholder={intl.formatMessage({ id: 'cart:enter-coupon', defaultMessage: 'Enter your code' })}
            />
            <Button priority="high" onClick={addCouponHandler} disabled={couponCode.length < 3}>
                <span>{intl.formatMessage({ id: 'cart:apply-coupon', defaultMessage: 'Apply Coupon' })}</span>
            </Button>
            {errorFragment}
        </form>
    );
};

export default CouponForm;
