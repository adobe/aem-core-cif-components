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
import { render, waitForElement, fireEvent } from '@testing-library/react';

import { CartProvider, useCartState } from '../cartContext';
import UserContextProvider from '../../../context/UserContext';
import CartInitializer from '../cartInitializer';

describe('Cart handlers', () => {
    const aSpy = jest.fn();
    const mockReducerFactory = fn => {
        return (state, action) => {
            switch (action.type) {
                case 'cartId':
                    return {
                        ...state,
                        cartId: action.cartId,
                        ...action.methods
                    };
                default: {
                    aSpy(action.type);
                }
            }

            return state;
        };
    };

    const renderWithContext = Component => {
        return (
            <MockedProvider mocks={[]} addTypename={false}>
                <UserContextProvider>
                    <CartProvider
                        initialState={{ cartId: 'guest123', cart: { id: 'guest123', items: [] } }}
                        reducerFactory={mockReducerFactory}>
                        <CartInitializer>
                            <Component />
                        </CartInitializer>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );
    };

    it('addItem', async () => {
        aSpy.mockClear();
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=guest123;path=/;domain=http://localhost;Max-Age=3600'
        });
        const MockComponent = () => {
            const [{ cartId, addItem }] = useCartState();

            return (
                <div>
                    <button
                        onClick={() => {
                            addItem({ detail: [{ sku: 'TEST', quantity: '1' }] });
                        }}>
                        Add item to cart
                    </button>
                    <div data-testid="cart-id">{cartId}</div>
                </div>
            );
        };

        const { getByTestId, getByRole, debug } = render(renderWithContext(MockComponent));
        fireEvent.click(getByRole('button'));
        const cartElement = await waitForElement(() => getByTestId('cart-id'));
        expect(aSpy).toHaveBeenCalledWith('open');
    });

    it('removeItem', async () => {
        aSpy.mockClear();
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=guest123;path=/;domain=http://localhost;Max-Age=3600'
        });
        const MockComponent = () => {
            const [{ cartId, removeItem }] = useCartState();

            return (
                <div>
                    <button
                        onClick={() => {
                            removeItem({ detail: { sku: 'TEST', quantity: '1' } });
                        }}>
                        Add item to cart
                    </button>
                    <div data-testid="cart-id">{cartId}</div>
                </div>
            );
        };

        const { getByTestId, getByRole } = render(renderWithContext(MockComponent));

        fireEvent.click(getByRole('button'));
        const cartElement = await waitForElement(() => getByTestId('cart-id'));
        expect(aSpy).toHaveBeenCalledWith('endLoading');
    });
});
