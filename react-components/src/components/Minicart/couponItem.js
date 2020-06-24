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
import { useTranslation } from 'react-i18next';

import Kebab from './kebab';
import Section from './section';

import classes from './couponItem.css';
import useCouponItem from './useCouponItem';

const CouponItem = () => {
    const [{ appliedCoupon }, { removeCouponFromCart }] = useCouponItem();

    const [t] = useTranslation('cart');

    return (
        <div className={classes.root}>
            <div className={classes.couponName}>
                Coupon <strong>{appliedCoupon}</strong> applied.
            </div>
            <Kebab>
                <Section text={t('cart:remove-coupon', 'Remove coupon')} onClick={removeCouponFromCart} icon="Trash" />
            </Kebab>
        </div>
    );
};

export default CouponItem;
