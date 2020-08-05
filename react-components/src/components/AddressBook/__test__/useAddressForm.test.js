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
import { render, fireEvent } from '@testing-library/react';

import UserContextProvider from '../../../context/UserContext';
import { useAddressForm } from '../useAddressForm';
import { MockedProvider } from '@apollo/react-testing';
import * as actions from '../../../actions/user';

jest.mock('../../../actions/user');

describe('useAddressForm', () => {
    it('calls the "updateAddress"', async () => {
        const Wrapper = () => {
            const { handleSubmit } = useAddressForm();

            return (
                <div>
                    <button onClick={handleSubmit}>Submit</button>
                </div>
            );
        };

        const mockInitialState = {
            updateAddress: {
                id: 'my-address-id'
            }
        };

        const { getByRole } = render(
            <MockedProvider>
                <UserContextProvider initialState={mockInitialState}>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.click(getByRole('button'));
        expect(actions.updateAddress).toHaveBeenCalledTimes(1);
    });

    it('calls the "createAddress"', async () => {
        const Wrapper = () => {
            const { handleSubmit } = useAddressForm();

            return (
                <div>
                    <button onClick={handleSubmit}>Submit</button>
                </div>
            );
        };

        const { getByRole } = render(
            <MockedProvider>
                <UserContextProvider>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.click(getByRole('button'));
        expect(actions.createAddress).toHaveBeenCalledTimes(1);
    });

    it('calls the handle cancel callback function with update address', async () => {
        const Wrapper = () => {
            const { handleCancel } = useAddressForm();

            return (
                <div>
                    <button onClick={handleCancel}>Cancel</button>
                </div>
            );
        };

        const mockInitialState = {
            updateAddress: {
                id: 'my-address-id'
            }
        };

        const handler = jest.fn(state => state);

        const { getByRole } = render(
            <MockedProvider>
                <UserContextProvider initialState={mockInitialState} reducerFactory={() => handler}>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.click(getByRole('button'));
        expect(handler).toHaveBeenCalledTimes(3);
    });

    it('calls the handle cancel callback function without update address', async () => {
        const Wrapper = () => {
            const { handleCancel } = useAddressForm();

            return (
                <div>
                    <button onClick={handleCancel}>Cancel</button>
                </div>
            );
        };

        const handler = jest.fn(state => state);

        const { getByRole } = render(
            <MockedProvider>
                <UserContextProvider reducerFactory={() => handler}>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.click(getByRole('button'));
        expect(handler).toHaveBeenCalledTimes(2);
    });

    it('get the value of the form error state', async () => {
        const Wrapper = () => {
            const { errorMessage } = useAddressForm();

            return <div data-testid="error">{errorMessage}</div>;
        };

        const mockInitialState = {
            addressFormError: 'address form error'
        };

        const { getByTestId } = render(
            <MockedProvider>
                <UserContextProvider initialState={mockInitialState}>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        const errorMessage = getByTestId('error');
        expect(errorMessage.textContent).toEqual('address form error');
    });

    it('get the correct region id', async () => {
        const Wrapper = () => {
            const { getRegionId } = useAddressForm();
            const countries = [
                {
                    id: 'US',
                    available_regions: [
                        { id: 4, code: 'AL', name: 'Alabama' },
                        { id: 7, code: 'AK', name: 'Alaska' }
                    ]
                }
            ];

            return <div data-testid="region-id">{getRegionId(countries, 'US', 'AL')}</div>;
        };

        const { getByTestId } = render(
            <MockedProvider>
                <UserContextProvider>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        const regionId = getByTestId('region-id');
        expect(regionId.textContent).toEqual('4');
    });
});
