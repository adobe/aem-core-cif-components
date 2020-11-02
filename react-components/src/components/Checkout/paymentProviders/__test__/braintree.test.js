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

jest.mock('braintree-web-drop-in', () => ({
    create: jest.fn()
}));
jest.mock('informed', () => ({
    useFieldApi: jest.fn()
}));

import React from 'react';
import { render, act, wait } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing';
import dropIn from 'braintree-web-drop-in';
import { useFieldApi } from 'informed';

import Braintree from '../braintree';
import { CartProvider } from '../../../Minicart/cartContext';
import { CheckoutProvider } from '../../checkoutContext';

import CREATE_BRAINTREE_CLIENT_TOKEN from '../../../../queries/mutation_create_braintree_client_token.graphql';

describe('<Braintree />', () => {
    beforeEach(() => {
        dropIn.create.mockReset();
    });

    it('requests a braintree token it is missing in the checkout context', async () => {
        const mocks = [
            {
                request: {
                    query: CREATE_BRAINTREE_CLIENT_TOKEN
                },
                result: {
                    data: {
                        createBraintreeClientToken: 'my-sample-token'
                    }
                }
            }
        ];

        let mockReducer = jest.fn(state => state);
        render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <CartProvider initialState={{}} reducerFactory={() => state => state}>
                    <CheckoutProvider initialState={{ flowState: '', braintreeToken: null }} reducer={mockReducer}>
                        <Braintree accept="card" />
                    </CheckoutProvider>
                </CartProvider>
            </MockedProvider>
        );

        // Wait for mutation call to be done
        await wait(() => mockReducer.mock.calls.length === 1);

        // Verify reducer call
        expect(mockReducer.mock.calls.length).toBe(1);
        expect(mockReducer.mock.calls[0][1].type).toBe('setBraintreeToken');
        expect(mockReducer.mock.calls[0][1].token).toBe('my-sample-token');
    });

    it('creates a new card dropin instance', async () => {
        let mockOnFn = jest.fn();
        dropIn.create.mockResolvedValue({
            on: mockOnFn
        });

        let mockCartDispatchFn = jest.fn(state => state);

        render(
            <MockedProvider>
                <CartProvider initialState={{}} reducerFactory={() => mockCartDispatchFn}>
                    <CheckoutProvider
                        initialState={{ flowState: '', braintreeToken: 'my-sample-token' }}
                        reducer={state => state}>
                        <Braintree accept="card" />
                    </CheckoutProvider>
                </CartProvider>
            </MockedProvider>
        );

        await wait(() => dropIn.create.mock.calls.length === 1 && mockOnFn.mock.calls.length === 2);

        // Don't expect and cart error dispatch calls
        expect(mockCartDispatchFn.mock.calls.length).toBe(0);

        // Expect one dropin create call
        expect(dropIn.create.mock.calls.length).toBe(1);
        const options = dropIn.create.mock.calls[0][0];
        expect(options.paypal).toBe(false);
        expect(options.card).not.toBe(false);

        // Expect two event handlers to be set
        expect(mockOnFn.mock.calls.length).toBe(2);
    });

    it('creates a new paypal dropin instance', async () => {
        let mockOnFn = jest.fn();
        dropIn.create.mockResolvedValue({
            on: mockOnFn
        });

        let mockCartDispatchFn = jest.fn(state => state);
        const cartState = {
            cart: {
                prices: {
                    grand_total: {
                        value: 100,
                        currency: 'EUR'
                    }
                }
            }
        };

        render(
            <MockedProvider>
                <CartProvider initialState={cartState} reducerFactory={() => mockCartDispatchFn}>
                    <CheckoutProvider
                        initialState={{ flowState: '', braintreeToken: 'my-sample-token' }}
                        reducer={state => state}>
                        <Braintree accept="paypal" />
                    </CheckoutProvider>
                </CartProvider>
            </MockedProvider>
        );

        await wait(() => dropIn.create.mock.calls.length === 1 && mockOnFn.mock.calls.length === 2);

        // Don't expect and cart error dispatch calls
        expect(mockCartDispatchFn.mock.calls.length).toBe(0);

        // Expect one dropin create call
        expect(dropIn.create.mock.calls.length).toBe(1);
        const options = dropIn.create.mock.calls[0][0];
        expect(options.paypal).not.toBe(false);
        expect(options.paypal.amount).toBe(100);
        expect(options.paypal.currency).toBe('EUR');
        expect(options.card).toBe(false);

        // Expect two event handlers to be set
        expect(mockOnFn.mock.calls.length).toBe(2);
    });

    it('requests a payment nonce', async () => {
        // Prepare mocks
        let mockSetValueFn = jest.fn();
        useFieldApi.mockImplementation(() => ({
            setValue: mockSetValueFn
        }));
        let mockOnFn = jest.fn();
        let mockRequestPaymentMethodFn = jest.fn().mockResolvedValue({ nonce: 'my-payment-nonce' });
        dropIn.create.mockResolvedValue({
            on: mockOnFn,
            requestPaymentMethod: mockRequestPaymentMethodFn
        });
        let mockCartDispatchFn = jest.fn(state => state);

        // Step 1, create Dropin
        render(
            <MockedProvider>
                <CartProvider initialState={{}} reducerFactory={() => mockCartDispatchFn}>
                    <CheckoutProvider
                        initialState={{ flowState: '', braintreeToken: 'my-sample-token' }}
                        reducer={state => state}>
                        <Braintree accept="card" />
                    </CheckoutProvider>
                </CartProvider>
            </MockedProvider>
        );

        await wait(() => dropIn.create.mock.calls.length === 1 && mockOnFn.mock.calls.length === 2);

        expect(dropIn.create.mock.calls.length).toBe(1);
        expect(mockOnFn.mock.calls.length).toBe(2);

        // Step 2, get registered event handler and call it
        const triggerFunction = mockOnFn.mock.calls[0][1];
        act(() => {
            triggerFunction();
        });

        // Step 3, verify payment nonce
        await wait(() => mockSetValueFn.mock.calls.length === 1);
        expect(mockSetValueFn.mock.calls.length).toBe(1);
        expect(mockSetValueFn.mock.calls[0][0]).toBe('my-payment-nonce');

        // Make sure no cart error was dispatched
        expect(mockCartDispatchFn.mock.calls.length).toBe(0);
    });
});
