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
import { useMutation } from '@apollo/client';

import { useCartState } from './cartContext';
import { addCoupon } from '../../actions/cart';
import { useAwaitQuery } from '../../utils/hooks';

import MUTATION_ADD_COUPON from '../../queries/mutation_add_coupon.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

export default () => {
    const [{ cartId, couponError }, dispatch] = useCartState();

    const [addCouponMutation] = useMutation(MUTATION_ADD_COUPON);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);

    const addCouponToCart = async couponCode => {
        dispatch({ type: 'beginLoading' });
        await addCoupon({
            cartId,
            couponCode,
            cartDetailsQuery,
            addCouponMutation,
            dispatch
        });

        dispatch({ type: 'endLoading' });
    };
    return [{ couponError }, { addCouponToCart }];
};
