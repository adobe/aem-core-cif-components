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
import React from 'react';
import { useIntl } from 'react-intl';

import Kebab from './kebab';
import Section from './section';

import classes from './couponItem.css';
import useCouponItem from './useCouponItem';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const CouponItem = () => {
    const [{ appliedCoupon }, { removeCouponFromCart }] = useCouponItem();
    const intl = useIntl();

    return (
        <div className={classes.root}>
            <div className={classes.couponName}>
                Coupon <strong>{appliedCoupon}</strong> applied.
            </div>
            <Kebab>
                <Section
                    text={intl.formatMessage({ id: 'cart:remove-coupon', defaultMessage: 'Remove coupon' })}
                    onClick={removeCouponFromCart}
                    icon="Trash"
                />
            </Kebab>
        </div>
    );
};

export default CouponItem;
