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
import { I18nextProvider } from 'react-i18next';
import { render, fireEvent, wait } from '@testing-library/react';

import { CheckoutProvider } from '../../Checkout/checkoutContext';
import UserContextProvider from '../../../context/UserContext';
import { useAddressSelect } from '../useAddressSelect';
import i18n from '../../../../__mocks__/i18nForTests';

describe('useAddressSelect', () => {
    const mockInitialState = {
        currentUser: {
            addresses: [
                {
                    id: '1',
                    city: 'Calder',
                    country_code: 'US',
                    firstname: 'Veronica',
                    lastname: 'Costello',
                    postcode: '49628-7978',
                    region: {
                        region_code: 'MI'
                    },
                    street: ['saved address street'],
                    telephone: '(555) 229-3326'
                }
            ]
        }
    };

    it('gets the "addressSelectItems" variable', async () => {
        const Wrapper = () => {
            const { addressSelectItems } = useAddressSelect();

            return (
                <>
                    <div data-testid="new-address-label">{addressSelectItems[0].label}</div>
                    <div data-testid="saved-address-label">{addressSelectItems[1].label}</div>
                </>
            );
        };

        const { getByTestId } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={mockInitialState}>
                        <CheckoutProvider>
                            <Wrapper />
                        </CheckoutProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        await wait(() => {
            const newAddressLabel = getByTestId('new-address-label');
            expect(newAddressLabel.textContent).toEqual('New Address');

            const savedAddressLabel = getByTestId('saved-address-label');
            expect(savedAddressLabel.textContent).toEqual('saved address street');
        });
    });

    it('calls the "handleChangeAddressSelectInCheckout" callback function', async () => {
        const setValues = jest.fn();
        const Wrapper = () => {
            const { handleChangeAddressSelectInCheckout } = useAddressSelect();

            return (
                <div>
                    <button
                        onClick={() => handleChangeAddressSelectInCheckout(0, { setValues })}
                        data-testid="change-to-new-address-item">
                        Change to new address
                    </button>

                    <button
                        onClick={() => handleChangeAddressSelectInCheckout(1, { setValues })}
                        data-testid="change-to-saved-address-item">
                        Change to new address
                    </button>
                </div>
            );
        };

        const handler = jest.fn(state => state);

        const { getByTestId } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={mockInitialState}>
                        <CheckoutProvider reducer={handler}>
                            <Wrapper />
                        </CheckoutProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        fireEvent.click(getByTestId('change-to-new-address-item'));
        await wait(() => {
            expect(setValues).toHaveBeenCalledTimes(1);
            expect(handler).toHaveBeenCalledTimes(1);
        });

        fireEvent.click(getByTestId('change-to-saved-address-item'));
        await wait(() => {
            expect(setValues).toHaveBeenCalledTimes(2);
            expect(handler).toHaveBeenCalledTimes(2);
        });
    });

    it('calls the "parseInitialAddressSelectValue" function', async () => {
        const Wrapper = () => {
            const { parseInitialAddressSelectValue } = useAddressSelect();
            const initialValue = parseInitialAddressSelectValue({
                city: 'Calder',
                country_code: 'US',
                firstname: 'Veronica',
                lastname: 'Costello',
                postcode: '49628-7978',
                region_code: 'MI',
                street: ['saved address street'],
                telephone: '(555) 229-3326'
            });

            return <div data-testid="address-select-initial-value">{initialValue}</div>;
        };

        const { getByTestId } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider initialState={mockInitialState}>
                        <CheckoutProvider>
                            <Wrapper />
                        </CheckoutProvider>
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        await wait(() => {
            const initialValue = getByTestId('address-select-initial-value');
            expect(initialValue.textContent).toEqual('1');
        });
    });
});
