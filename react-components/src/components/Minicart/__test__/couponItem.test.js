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
import { MockedProvider } from '@apollo/react-testing';

import { render, fireEvent } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';

import { CartProvider } from '../cartContext';
import CouponItem from '../couponItem';
import i18n from '../../../../__mocks__/i18nForTests';

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
            <I18nextProvider i18n={i18n}>
                <MockedProvider mocks={[]}>
                    <CartProvider initialState={initialState} reducerFactory={() => state => state}>
                        <CouponItem />
                    </CartProvider>
                </MockedProvider>
            </I18nextProvider>
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
            <I18nextProvider i18n={i18n}>
                <MockedProvider mocks={[]}>
                    <CartProvider initialState={initialState} reducerFactory={() => state => state}>
                        <CouponItem />
                    </CartProvider>
                </MockedProvider>
            </I18nextProvider>
        );
        fireEvent.mouseDown(getByText('Remove coupon'));

        expect(mockRemoveCouponFromCart).toHaveBeenCalledTimes(1);
    });
});
