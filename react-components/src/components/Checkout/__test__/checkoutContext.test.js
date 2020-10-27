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
import { fireEvent, waitForElement } from '@testing-library/react';
import { render } from 'test-utils';
import { CheckoutProvider, useCheckoutState } from '../checkoutContext';

describe('checkoutContext', () => {
    it('sets shipping address email in state', async () => {
        const ContextWrapper = () => {
            const [{ shippingAddress }, dispatch] = useCheckoutState();

            let content;
            if (shippingAddress.email) {
                content = (
                    <>
                        <div data-testid="shipping-address-street">{shippingAddress.street}</div>
                        <div data-testid="shipping-address-email">{shippingAddress.email}</div>
                    </>
                );
            } else {
                content = (
                    <button onClick={() => dispatch({ type: 'setShippingAddressEmail', email: 'test@example.com' })}>
                        Set Shipping Address Email
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const mockCheckoutState = {
            flowState: '',
            shippingAddress: {
                street: 'shipping address street',
                email: null
            }
        };

        const { getByRole, getByTestId } = render(
            <CheckoutProvider initialState={mockCheckoutState}>
                <ContextWrapper />
            </CheckoutProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const shippingAddressStreet = await waitForElement(() => getByTestId('shipping-address-street'));
        expect(shippingAddressStreet).not.toBeUndefined();
        expect(shippingAddressStreet.textContent).toEqual('shipping address street');

        const shippingAddressEmail = await waitForElement(() => getByTestId('shipping-address-email'));
        expect(shippingAddressEmail).not.toBeUndefined();
        expect(shippingAddressEmail.textContent).toEqual('test@example.com');
    });

    it('sets billing address email in state', async () => {
        const ContextWrapper = () => {
            const [{ billingAddress }, dispatch] = useCheckoutState();

            let content;
            if (billingAddress.email) {
                content = (
                    <>
                        <div data-testid="billing-address-street">{billingAddress.street}</div>
                        <div data-testid="billing-address-email">{billingAddress.email}</div>
                    </>
                );
            } else {
                content = (
                    <button onClick={() => dispatch({ type: 'setBillingAddressEmail', email: 'test@example.com' })}>
                        Set Billing Address Email
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const mockCheckoutState = {
            flowState: '',
            billingAddress: {
                street: 'billing address street',
                email: null
            }
        };

        const { getByRole, getByTestId } = render(
            <CheckoutProvider initialState={mockCheckoutState}>
                <ContextWrapper />
            </CheckoutProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const billingAddressStreet = await waitForElement(() => getByTestId('billing-address-street'));
        expect(billingAddressStreet).not.toBeUndefined();
        expect(billingAddressStreet.textContent).toEqual('billing address street');

        const billingAddressEmail = await waitForElement(() => getByTestId('billing-address-email'));
        expect(billingAddressEmail).not.toBeUndefined();
        expect(billingAddressEmail.textContent).toEqual('test@example.com');
    });

    it('sets billing address same as shipping address in state', async () => {
        const ContextWrapper = () => {
            const [{ billingAddressSameAsShippingAddress }, dispatch] = useCheckoutState();

            let content;
            if (billingAddressSameAsShippingAddress) {
                content = (
                    <div data-testid="billing-address-same-as-shipping-address">
                        {billingAddressSameAsShippingAddress ? 'true' : 'false'}
                    </div>
                );
            } else {
                content = (
                    <button onClick={() => dispatch({ type: 'setBillingAddressSameAsShippingAddress', same: true })}>
                        Set Billing Address Same as Shipping Address
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const mockCheckoutState = {
            flowState: '',
            billingAddressSameAsShippingAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CheckoutProvider initialState={mockCheckoutState}>
                <ContextWrapper />
            </CheckoutProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const billingAddressSameAsShippingAddress = await waitForElement(() =>
            getByTestId('billing-address-same-as-shipping-address')
        );
        expect(billingAddressSameAsShippingAddress).not.toBeUndefined();
        expect(billingAddressSameAsShippingAddress.textContent).toEqual('true');
    });

    it('sets is editing new address in state', async () => {
        const ContextWrapper = () => {
            const [{ isEditingNewAddress }, dispatch] = useCheckoutState();

            let content;
            if (isEditingNewAddress) {
                content = <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>;
            } else {
                content = (
                    <button onClick={() => dispatch({ type: 'setIsEditingNewAddress', editing: true })}>
                        Set is Editing New Address
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const mockCheckoutState = {
            flowState: '',
            isEditingNewAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CheckoutProvider initialState={mockCheckoutState}>
                <ContextWrapper />
            </CheckoutProvider>
        );

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const isEditingNewAddress = await waitForElement(() => getByTestId('is-editing-new-address'));
        expect(isEditingNewAddress).not.toBeUndefined();
        expect(isEditingNewAddress.textContent).toEqual('true');
    });
});
