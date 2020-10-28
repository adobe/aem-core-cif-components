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
import { CartProvider } from '../../Minicart';
import { CheckoutProvider, useCheckoutState } from '../checkoutContext';
import useOverview from '../useOverview';

describe('useOverview', () => {
    const mockShippingAddress = {
        city: 'Calder',
        country_code: 'US',
        company: 'shipping address company',
        firstname: 'Veronica',
        lastname: 'Costello',
        postcode: '49628-7978',
        region_code: 'MI',
        save_in_address_book: false,
        street: ['cart shipping address'],
        telephone: '(555) 229-3326'
    };

    it('edits new shipping address', async () => {
        const Wrapper = () => {
            const [, { editShippingAddress }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress, firstname: 'firstname' };

            let content;
            if (isEditingNewAddress) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editShippingAddress(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const isEditingNewAddress = await waitForElement(() => getByTestId('is-editing-new-address'));
        expect(isEditingNewAddress).not.toBeUndefined();
        expect(isEditingNewAddress.textContent).toEqual('true');

        const editing = getByTestId('editing');
        expect(editing).not.toBeUndefined();
        expect(editing.textContent).toEqual('address');
    });

    it('edits saved shipping address', async () => {
        const Wrapper = () => {
            const [, { editShippingAddress }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress };

            let content;
            if (editing) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editShippingAddress(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const isEditingNewAddress = await waitForElement(() => getByTestId('is-editing-new-address'));
        expect(isEditingNewAddress).not.toBeUndefined();
        expect(isEditingNewAddress.textContent).toEqual('false');

        const editing = getByTestId('editing');
        expect(editing).not.toBeUndefined();
        expect(editing.textContent).toEqual('address');
    });

    it('edits billing information with the billing address that is the same as the shipping address', async () => {
        const Wrapper = () => {
            const [, { editBillingInformation }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress };

            let content;
            if (isEditingNewAddress) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editBillingInformation(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false,
            billingAddressSameAsShippingAddress: true
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const isEditingNewAddress = await waitForElement(() => getByTestId('is-editing-new-address'));
        expect(isEditingNewAddress).not.toBeUndefined();
        expect(isEditingNewAddress.textContent).toEqual('true');

        const editing = getByTestId('editing');
        expect(editing).not.toBeUndefined();
        expect(editing.textContent).toEqual('paymentMethod');
    });

    it('edits billing information with saved billing address', async () => {
        const Wrapper = () => {
            const [, { editBillingInformation }] = useOverview();
            const [{ editing, isEditingNewAddress }] = useCheckoutState();
            const address = { ...mockShippingAddress };

            let content;
            if (editing) {
                content = (
                    <>
                        <div data-testid="is-editing-new-address">{isEditingNewAddress ? 'true' : 'false'}</div>
                        <div data-testid="editing">{editing}</div>
                    </>
                );
            } else {
                content = <button onClick={() => editBillingInformation(address)}>Edit Shipping Address</button>;
            }

            return <div>{content}</div>;
        };

        const mockUserState = {
            currentUser: {
                addresses: [{ ...mockShippingAddress }]
            }
        };

        const mockCheckoutState = {
            flowState: '',
            editing: null,
            isEditingNewAddress: false,
            billingAddressSameAsShippingAddress: false
        };

        const { getByRole, getByTestId } = render(
            <CartProvider>
                <CheckoutProvider initialState={mockCheckoutState}>
                    <Wrapper />
                </CheckoutProvider>
            </CartProvider>,
            { userContext: mockUserState }
        );

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const isEditingNewAddress = await waitForElement(() => getByTestId('is-editing-new-address'));
        expect(isEditingNewAddress).not.toBeUndefined();
        expect(isEditingNewAddress.textContent).toEqual('false');

        const editing = getByTestId('editing');
        expect(editing).not.toBeUndefined();
        expect(editing.textContent).toEqual('paymentMethod');
    });
});
