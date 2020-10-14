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
import ProductList from '../productList';
import { render } from '../../../utils/test-utils';
import { CartProvider } from '../cartContext';

describe('<ProductList>', () => {
    const mockCartItems = [
        {
            product: {
                thumbnail: {
                    url: '/some/url'
                },
                name: 'Some t-shirt'
            },
            prices: {
                price: {
                    currency: 'USD',
                    value: 1
                },
                row_total: {
                    currency: 'USD',
                    value: 1
                }
            },
            quantity: 2,
            id: '1'
        },
        {
            product: {
                thumbnail: {
                    url: '/some/url'
                },
                name: 'Some shorts'
            },
            prices: {
                price: {
                    currency: 'USD',
                    value: 30
                },
                row_total: {
                    currency: 'USD',
                    value: 30
                }
            },
            quantity: 2,
            id: '2'
        }
    ];

    it('renders a list of products', () => {
        const { getAllByTestId } = render(
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <ProductList cartItems={mockCartItems} removeItemFromCart={jest.fn()} />
            </CartProvider>
        );

        const productItems = getAllByTestId('cart-item');
        expect(productItems).toHaveLength(2);
    });
});
