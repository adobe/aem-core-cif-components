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
import { render, fireEvent } from '@testing-library/react';
import { CartProvider } from '../cartContext';
import CouponForm from '../couponForm';

describe('<CouponForm />', () => {
    it('renders the component', () => {
        const { asFragment } = render(
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <CouponForm />
            </CartProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders an error', () => {
        const initialState = {
            couponError: 'Invalid coupon!'
        };

        const { asFragment } = render(
            <CartProvider initialState={initialState} reducerFactory={() => state => state}>
                <CouponForm />
            </CartProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('applies an coupon', () => {
        const mockFn = jest.fn();

        const initialState = {
            addCoupon: mockFn
        };

        const { getByText, getByPlaceholderText } = render(
            <CartProvider initialState={initialState} reducerFactory={() => state => state}>
                <CouponForm />
            </CartProvider>
        );

        // Add coupon to input
        fireEvent.change(getByPlaceholderText('Enter your code'), { target: { value: 'my-coupon' } });

        // Click on button
        fireEvent.click(getByText('Apply Coupon'));

        // Expect mock function to be called with coupon
        expect(mockFn.mock.calls.length).toEqual(1);
        expect(mockFn.mock.calls[0][0]).toBe('my-coupon');
    });
});
