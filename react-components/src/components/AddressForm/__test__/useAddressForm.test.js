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
import { render, fireEvent } from '@testing-library/react';

import UserContextProvider from '../../../context/UserContext';
import { useAddressForm } from '../useAddressForm';
import * as actions from '../../../actions/user';

jest.mock('../../../actions/user');

const mockReducerFactory = jest.fn(state => state);

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

    it('gets the value of the form error state', async () => {
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

    it('gets the correct region id', async () => {
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

    it('gets the new address object', async () => {
        const Wrapper = () => {
            const { getNewAddress } = useAddressForm();
            const newAddress = getNewAddress();
            return (
                <>
                    <div data-testid="city">{newAddress.city}</div>
                    <div data-testid="firstname">{newAddress.firstname}</div>
                    <div data-testid="lastname">{newAddress.lastname}</div>
                    <div data-testid="postcode">{newAddress.postcode}</div>
                    <div data-testid="region_code">{newAddress.region_code}</div>
                    <div data-testid="street0">{newAddress.street0}</div>
                    <div data-testid="telephone">{newAddress.telephone}</div>
                </>
            );
        };

        const { getByTestId } = render(
            <MockedProvider>
                <UserContextProvider>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        const city = getByTestId('city');
        expect(city.textContent).toEqual('');

        const firstname = getByTestId('firstname');
        expect(firstname.textContent).toEqual('');

        const lastname = getByTestId('lastname');
        expect(lastname.textContent).toEqual('');

        const postcode = getByTestId('postcode');
        expect(postcode.textContent).toEqual('');

        const region_code = getByTestId('region_code');
        expect(region_code.textContent).toEqual('');

        const street0 = getByTestId('street0');
        expect(street0.textContent).toEqual('');

        const telephone = getByTestId('telephone');
        expect(telephone.textContent).toEqual('');
    });

    it('parses the address', async () => {
        const Wrapper = () => {
            const address = {
                city: 'Calder',
                country_code: 'US',
                firstname: 'Veronica',
                lastname: 'Costello',
                postcode: '49628-7978',
                region: {
                    region_code: 'MI'
                },
                telephone: '(555) 229-3326'
            };
            const email = 'veronica@example.com';
            const { parseAddress } = useAddressForm();
            const parsedAddress = parseAddress(address, email);

            return (
                <>
                    <div data-testid="city">{parsedAddress.city}</div>
                    <div data-testid="country_code">{parsedAddress.country_code}</div>
                    <div data-testid="email">{parsedAddress.email}</div>
                    <div data-testid="firstname">{parsedAddress.firstname}</div>
                    <div data-testid="lastname">{parsedAddress.lastname}</div>
                    <div data-testid="postcode">{parsedAddress.postcode}</div>
                    <div data-testid="region_code">{parsedAddress.region_code}</div>
                    <div data-testid="telephone">{parsedAddress.telephone}</div>
                </>
            );
        };

        const { getByTestId } = render(
            <MockedProvider>
                <UserContextProvider>
                    <Wrapper />
                </UserContextProvider>
            </MockedProvider>
        );

        const city = getByTestId('city');
        expect(city.textContent).toEqual('Calder');

        const country_code = getByTestId('country_code');
        expect(country_code.textContent).toEqual('US');

        const email = getByTestId('email');
        expect(email.textContent).toEqual('veronica@example.com');

        const firstname = getByTestId('firstname');
        expect(firstname.textContent).toEqual('Veronica');

        const lastname = getByTestId('lastname');
        expect(lastname.textContent).toEqual('Costello');

        const postcode = getByTestId('postcode');
        expect(postcode.textContent).toEqual('49628-7978');

        const region_code = getByTestId('region_code');
        expect(region_code.textContent).toEqual('MI');

        const telephone = getByTestId('telephone');
        expect(telephone.textContent).toEqual('(555) 229-3326');
    });
});
