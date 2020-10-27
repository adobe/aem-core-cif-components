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
import { fireEvent } from '@testing-library/react';
import { render } from 'test-utils';
import * as actions from '../../../actions/cart';
import useCouponForm from '../useCouponForm';

// mock the actions because we don't need them
jest.mock('../../../actions/cart');

// mock the cart context to make the whole stack lighter
jest.mock('../../Minicart/cartContext.js', () => ({
    useCartState: () => {
        return [{ cartId: null, cart: {} }, jest.fn()];
    }
}));

describe('useCouponForm', () => {
    it('calls the "addCoupon" action', () => {
        const Consumer = () => {
            const [, { addCouponToCart }] = useCouponForm();
            const addCouponHandler = () => {
                return addCouponToCart('c123');
            };
            return (
                <div>
                    <button onClick={addCouponHandler}>Add it</button>
                </div>
            );
        };

        const { getByRole } = render(<Consumer />);

        fireEvent.click(getByRole('button'));

        expect(actions.addCoupon).toHaveBeenCalledTimes(1);
        expect(actions.addCoupon.mock.calls[0][0]).toHaveProperty('couponCode', 'c123');
    });
});
