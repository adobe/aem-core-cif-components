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
import { render } from 'test-utils';
import { fireEvent } from '@testing-library/react';
import { CartProvider } from '../cartContext';
import CouponItem from '../couponItem';

const mockRemoveCouponFromCart = jest.fn();
jest.mock('../useCouponItem.js', () => {
    return jest.fn().mockImplementation(() => {
        return [{ appliedCoupon: 'my-sample-coupon' }, { removeCouponFromCart: mockRemoveCouponFromCart }];
    });
});

describe('<CouponItem />', () => {
    it('renders the component', () => {
        const initialState = {
            cart: {
                applied_coupon: {
                    code: 'my-sample-coupon'
                }
            }
        };

        const { asFragment } = render(
            <CartProvider initialState={initialState} reducerFactory={() => state => state}>
                <CouponItem />
            </CartProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('calls the appropriate function to remove the coupon', () => {
        const initialState = {
            cart: {
                applied_coupon: {
                    code: 'my-sample-coupon'
                }
            }
        };

        const { getByText } = render(
            <CartProvider initialState={initialState} reducerFactory={() => state => state}>
                <CouponItem />
            </CartProvider>
        );
        fireEvent.mouseDown(getByText('Remove coupon'));
        expect(mockRemoveCouponFromCart).toHaveBeenCalledTimes(1);
    });
});
