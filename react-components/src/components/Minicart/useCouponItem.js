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
import { removeCoupon } from '../../actions/cart';
import { useAwaitQuery } from '../../utils/hooks';

import MUTATION_REMOVE_COUPON from '../../queries/mutation_remove_coupon.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

export default () => {
    const [{ cartId, cart }, dispatch] = useCartState();
    const [removeCouponMutation] = useMutation(MUTATION_REMOVE_COUPON);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);

    const appliedCoupon = cart.applied_coupon ? cart.applied_coupon.code : null;

    const removeCouponFromCart = async () => {
        dispatch({ type: 'beginLoading' });
        await removeCoupon({ cartId, removeCouponMutation, cartDetailsQuery, dispatch });
        dispatch({ type: 'endLoading' });
    };

    return [{ appliedCoupon }, { removeCouponFromCart }];
};
