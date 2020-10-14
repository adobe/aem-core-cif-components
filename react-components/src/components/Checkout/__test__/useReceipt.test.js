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
import { fireEvent } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import { CheckoutProvider } from '../checkoutContext';
import useReceipt from '../useReceipt';

describe('useReceipt', () => {
    it('resets the checkout context', async () => {
        const MockCmp = () => {
            const [{ orderId }, continueShopping] = useReceipt();

            return (
                <>
                    <div data-testid="orderId">{orderId}</div>
                    <button onClick={continueShopping}>Continue Shopping</button>
                </>
            );
        };

        const { getByTestId, getByRole } = render(
            <CheckoutProvider initialState={{ flowState: 'receipt', order: { order_id: 'my-order' } }}>
                <MockCmp />
            </CheckoutProvider>
        );

        const orderId = getByTestId('orderId');
        expect(orderId.textContent).toEqual('my-order');

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        expect(orderId.textContent).toBe('');
    });
});
