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
import Product from '../product';
import { render } from 'test-utils';
import { CartProvider } from '../cartContext';

const mockCartItem = {
    product: {
        thumbnail: {
            url: '/some/url'
        },
        name
    },
    prices: {
        price: {
            currency: 'USD',
            value: 22
        },
        row_total: {
            currency: 'USD',
            value: 22
        }
    },
    quantity: 2,
    id: '1'
};

describe('<Product />', () => {
    it('renders the component', () => {
        const mockRemoveItemFromCart = jest.fn();
        const mockBeginEditItem = jest.fn();

        const { asFragment } = render(
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <Product
                    beginEditItem={mockBeginEditItem}
                    removeItemFromCart={mockRemoveItemFromCart}
                    item={mockCartItem}
                />
            </CartProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });
});
