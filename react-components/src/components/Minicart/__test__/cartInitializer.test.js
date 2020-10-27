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
import { waitForElement, fireEvent } from '@testing-library/react';
import { render } from 'test-utils';
import { useCartState, CartProvider } from '../cartContext';
import CartInitializer from '../cartInitializer';
import { useUserContext } from '../../..';

const DummyCart = () => {
    const [{ cartId }] = useCartState();
    const [{ isSignedIn }, { signIn }] = useUserContext();
    if (!cartId || cartId.length === 0) {
        return <div>No cart</div>;
    }

    // we add this element to detect if the cart id has been updated.
    // This basically moves the assertion to the component
    let successElement = cartId === 'customercart' ? <div data-testid="success"></div> : '';
    return (
        <div>
            <div data-testid="cart-details">{cartId}</div>
            {successElement}
            {isSignedIn && <div data-testid="signed-in"></div>}
            <button
                data-testid="sign-in"
                onClick={() => {
                    signIn('', '');
                }}>
                Sign in
            </button>
        </div>
    );
};

describe('<CartInitializer />', () => {
    it('retrieves the cartId from the cookie', async () => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=cart-from-cookie;path=/;domain=http://localhost;Max-Age=3600'
        });

        const { getByTestId } = render(
            <CartProvider
                initialState={{ cartId: null }}
                reducerFactory={() => (state, action) => {
                    if (action.type === 'cartId') {
                        return { ...state, cartId: action.cartId };
                    }
                    return state;
                }}>
                <CartInitializer>
                    <DummyCart />
                </CartInitializer>
            </CartProvider>
        );
        const cartIdNode = await waitForElement(() => getByTestId('cart-details'));

        expect(cartIdNode.textContent).toEqual('cart-from-cookie');
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: ''
        });
    });

    it('resets the cart id after a checkout', async () => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=oldcustomercart;path=/;domain=http://localhost;Max-Age=3600'
        });
        const ResetCartComponent = () => {
            const [{ cartId }, dispatch] = useCartState();

            if (!cartId || cartId.length === 0) {
                return <div data-testid="cart-id">none</div>;
            }
            return (
                <div>
                    <button
                        onClick={() => {
                            dispatch({ type: 'reset' });
                        }}>
                        Reset
                    </button>
                </div>
            );
        };

        const { getByTestId, getByRole } = render(
            <CartProvider>
                <CartInitializer>
                    <ResetCartComponent />
                </CartInitializer>
            </CartProvider>
        );

        fireEvent.click(getByRole('button'));
        const cartIdNode = await waitForElement(() => {
            return getByTestId('cart-id');
        });
        expect(cartIdNode.textContent).toEqual('none');
    });
});
