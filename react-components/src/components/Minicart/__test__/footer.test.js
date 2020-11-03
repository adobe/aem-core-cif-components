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
import { render } from '../../../utils/test-utils';
import { CartProvider } from '../cartContext';
import Footer from '../footer';

jest.mock('../../Checkout', () => {
    return () => {
        return null;
    };
});
describe('<Footer>', () => {
    it('renders the component', () => {
        const { asFragment } = render(
            <CartProvider
                initialState={{
                    cart: {
                        prices: {
                            subtotal_excluding_tax: { currency: 'USD', value: 60 },
                            subtotal_with_discount_excluding_tax: { currency: 'USD', value: 34 }
                        },
                        total_quantity: 3
                    }
                }}
                reducerFactory={() => state => state}>
                <Footer />
            </CartProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with null cart', () => {
        const { asFragment } = render(
            <CartProvider
                initialState={{
                    cart: null
                }}
                reducerFactory={() => state => state}>
                <Footer />
            </CartProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
