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
import { render, waitForElement } from '@testing-library/react';

import MUTATION_CREATE_CART from '../../../queries/mutation_create_guest_cart.graphql';

import { useCartState, CartProvider } from '../cartContext';
import CartInitializer from '../cartInitializer';

const DummyCart = () => {
    const [{ cartId }] = useCartState();
    if (!cartId || cartId.length === 0) {
        return <div data-testid="cart-details">No cart</div>;
    }
    return (
        <div>
            <div data-testid="cart-details">{cartId}</div>
        </div>
    );
};

describe('<CartInitializer />', () => {
    // TODO: Functionality moved to CartInitializer, create new test
    it('retrieves the cartId from the cookie', async () => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=cart-from-cookie;path=/;domain=http://localhost;Max-Age=3600'
        });

        const { getByTestId } = render(
            <MockedProvider mocks={[]} addTypename={false}>
                <CartProvider
                    initialState={{ cartId: null }}
                    reducerFactory={() => (state, action) => {
                        if (action.type == 'cartId') {
                            return { ...state, cartId: action.cartId };
                        }
                        return state;
                    }}>
                    <CartInitializer>
                        <DummyCart />
                    </CartInitializer>
                </CartProvider>
            </MockedProvider>
        );
        const cartIdNode = await waitForElement(() => getByTestId('cart-details'));

        expect(cartIdNode.textContent).toEqual('cart-from-cookie');
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: ''
        });
    });

    it('creates a new cart if cartId is not set in cookie', async () => {
        const { getByTestId } = render(
            <MockedProvider
                mocks={[
                    {
                        request: {
                            query: MUTATION_CREATE_CART
                        },
                        result: {
                            data: {
                                createEmptyCart: 'guest123'
                            }
                        }
                    }
                ]}
                addTypename={false}>
                <CartProvider
                    initialState={{ cartId: null }}
                    reducerFactory={() => (state, action) => {
                        if (action.type == 'cartId') {
                            return { ...state, cartId: action.cartId };
                        }
                        return state;
                    }}>
                    <CartInitializer>
                        <DummyCart />
                    </CartInitializer>
                </CartProvider>
            </MockedProvider>
        );
        const cartIdNode = await waitForElement(() => getByTestId('cart-details'));

        expect(cartIdNode.children[0].textContent).toEqual('guest123');
    });
});
