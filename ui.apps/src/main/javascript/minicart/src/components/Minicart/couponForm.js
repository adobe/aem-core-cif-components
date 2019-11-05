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
import { useMutation } from '@apollo/react-hooks';

import classes from './couponForm.css';

import MUTATION_ADD_COUPON from '../../queries/mutation_add_coupon.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

const CouponForm = props => {
    const [{ cartId }, dispatch] = useCartState();
    const [couponCode, setCouponCode] = useState('');

    const [addCoupon] = useMutation(MUTATION_ADD_COUPON);

    const addCouponHandler = () => {
        dispatch({ type: 'beginLoading' });
        return addCoupon({
            variables: { cartId, couponCode },
            refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }],
            awaitRefetchQueries: true
        }).finally(() => {
            dispatch({ type: 'endLoading' });
        });
    };

    return (
        <form className={classes.root}>
            <input
                value={couponCode}
                onChange={e => setCouponCode(e.target.value)}
                type="text"
                name="couponCode"
                placeholder="Enter your code"
            />
            <Button priority="high" onClick={addCouponHandler}>
                <span>Apply Coupon</span>
            </Button>
        </form>
    );
};

export default CouponForm;
