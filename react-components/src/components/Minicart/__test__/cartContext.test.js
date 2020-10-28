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

/* eslint-disable react/prop-types */

import React from 'react';
import { CartProvider, useCartState } from '../cartContext';
import { fireEvent } from '@testing-library/react';
import { render } from '../../../utils/test-utils';

// avoid console errors logged during testing
console.error = jest.fn();

describe('CartContext', () => {
    const MockConsumer = ({ action }) => {
        const [state, dispatch] = useCartState();
        return (
            <div>
                <button
                    onClick={() => {
                        dispatch(action);
                    }}>
                    Dispatch!
                </button>
                <div data-testid="state">{JSON.stringify(state)}</div>
            </div>
        );
    };

    const renderAndGetResult = action => {
        const { getByRole, getByTestId } = render(
            <CartProvider>
                <MockConsumer action={action} />
            </CartProvider>
        );

        fireEvent.click(getByRole('button'));
        const stateNode = getByTestId('state');
        return stateNode.textContent;
    };

    it('dispatches the "open" action', () => {
        const result = renderAndGetResult({ type: 'open' });
        expect(result).toContain('"isOpen":true');
    });

    it('dispatches the "error" action', () => {
        const result = renderAndGetResult({ type: 'error', error: 'Generic error' });
        expect(result).toContain('Generic error');
    });
    it('dispatches the "beginLoading" action', () => {
        const result = renderAndGetResult({ type: 'beginLoading' });
        expect(result).toContain('"isLoading":true');
    });
    it('dispatches the "beginEditing" action', () => {
        const result = renderAndGetResult({ type: 'beginEditing', item: 'generic item' });
        expect(result).toContain('"isEditing":true');
        expect(result).toContain('"editItem":"generic item"');
    });

    it('dispatches the "cartId" action', () => {
        const result = renderAndGetResult({ type: 'cartId', cartId: 'guest123' });
        expect(result).toContain('guest123');
    });

    it('dispatches the "couponError" action', () => {
        const result = renderAndGetResult({ type: 'couponError', error: 'Coupon error' });
        expect(result).toContain('Coupon error');
    });
});
