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
import { fireEvent, wait } from '@testing-library/react';
import { render } from 'test-utils';
import UserContextProvider from '../../../context/UserContext';
import { useAddressForm } from '../useAddressForm';
import * as actions from '../../../actions/user';

jest.mock('../../../actions/user');

describe('useAddressForm', () => {
    it('calls the "updateAddress"', async () => {
        const Wrapper = () => {
            const { handleSubmit } = useAddressForm();
            const formValues = {
                default_shipping: true
            };

            return (
                <div>
                    <button onClick={() => handleSubmit(formValues)}>Submit</button>
                </div>
            );
        };

        const mockInitialState = {
            updateAddress: {
                id: 'my-address-id'
            }
        };

        const { getByRole } = render(<Wrapper />, { userContext: mockInitialState });

        fireEvent.click(getByRole('button'));
        await wait(() => {
            expect(actions.updateAddress).toHaveBeenCalledTimes(1);
        });
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

        const { getByRole } = render(<Wrapper />);

        fireEvent.click(getByRole('button'));
        await wait(() => {
            expect(actions.createAddress).toHaveBeenCalledTimes(1);
        });
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
            <UserContextProvider initialState={mockInitialState} reducerFactory={() => handler}>
                <Wrapper />
            </UserContextProvider>
        );

        fireEvent.click(getByRole('button'));
        await wait(() => {
            expect(handler).toHaveBeenCalledTimes(3);
        });
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
            <UserContextProvider reducerFactory={() => handler}>
                <Wrapper />
            </UserContextProvider>
        );

        fireEvent.click(getByRole('button'));
        await wait(() => {
            expect(handler).toHaveBeenCalledTimes(2);
        });
    });

    it('gets the value of the form error state', async () => {
        const Wrapper = () => {
            const { errorMessage } = useAddressForm();

            return <div data-testid="error">{errorMessage}</div>;
        };

        const { getByTestId } = render(<Wrapper />, { userContext: { addressFormError: 'address form error' } });

        await wait(() => {
            const errorMessage = getByTestId('error');
            expect(errorMessage.textContent).toEqual('address form error');
        });
    });

    it('finds the saved address', async () => {
        const Wrapper = () => {
            const { findSavedAddress } = useAddressForm();
            const address = {
                city: 'Calder',
                country_code: 'US',
                firstname: 'Veronica',
                lastname: 'Costello',
                postcode: '49628-7978',
                region_code: 'MI',
                street: ['saved address street'],
                telephone: '(555) 229-3326'
            };
            const foundAddress = findSavedAddress(address);

            return <div data-testid="street">{foundAddress.street}</div>;
        };

        const mockInitialState = {
            currentUser: {
                addresses: [
                    {
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

        const { getByTestId } = render(<Wrapper />, { userContext: mockInitialState });

        await wait(() => {
            const street = getByTestId('street');
            expect(street.textContent).toEqual('saved address street');
        });
    });

    it('gets the region code from an address', async () => {
        const Wrapper = () => {
            const { getRegionCode } = useAddressForm();
            const address1 = {
                region: {
                    code: 'AL'
                }
            };
            const address2 = {
                region: {
                    region_code: 'MI'
                }
            };

            const address3 = {
                region_code: 'LA'
            };

            return (
                <>
                    <div data-testid="region-code1">{getRegionCode(address1)}</div>
                    <div data-testid="region-code2">{getRegionCode(address2)}</div>
                    <div data-testid="region-code3">{getRegionCode(address3)}</div>
                </>
            );
        };

        const { getByTestId } = render(<Wrapper />);

        await wait(() => {
            const regionCode1 = getByTestId('region-code1');
            expect(regionCode1.textContent).toEqual('AL');

            const regionCode2 = getByTestId('region-code2');
            expect(regionCode2.textContent).toEqual('MI');

            const regionCode3 = getByTestId('region-code3');
            expect(regionCode3.textContent).toEqual('LA');
        });
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

        const { getByTestId } = render(<Wrapper />);

        await wait(() => {
            const regionId = getByTestId('region-id');
            expect(regionId.textContent).toEqual('4');
        });
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

        const { getByTestId } = render(<Wrapper />);

        await wait(() => {
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
    });

    it('parses the address with email', async () => {
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

        const { getByTestId } = render(<Wrapper />);

        await wait(() => {
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

    it('parses the address without email', async () => {
        const Wrapper = () => {
            const address = {
                city: 'Calder',
                country: {
                    code: 'US'
                },
                firstname: 'Veronica',
                lastname: 'Costello',
                postcode: '49628-7978',
                region: {
                    code: 'MI'
                },
                telephone: '(555) 229-3326'
            };
            const { parseAddress } = useAddressForm();
            const parsedAddress = parseAddress(address);

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

        const { getByTestId } = render(<Wrapper />);

        await wait(() => {
            const city = getByTestId('city');
            expect(city.textContent).toEqual('Calder');

            const country_code = getByTestId('country_code');
            expect(country_code.textContent).toEqual('US');

            const email = getByTestId('email');
            expect(email.textContent).toEqual('');

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

    it('parses the address form values', async () => {
        const Wrapper = () => {
            const address = {
                city: 'Calder',
                default_shipping: 'true',
                email: 'veronica@example.com',
                firstname: 'Veronica',
                lastname: 'Costello',
                postcode: '49628-7978',
                region_code: 'LA',
                region: {
                    code: 'MI'
                },
                save_in_address_book: 'true',
                street: ['address street'],
                telephone: '(555) 229-3326'
            };
            const { parseAddressFormValues } = useAddressForm();
            const parsedValues = parseAddressFormValues(address);

            return (
                <>
                    <div data-testid="city">{parsedValues.city}</div>
                    <div data-testid="default_shipping">{parsedValues.default_shipping}</div>
                    <div data-testid="email">{parsedValues.email}</div>
                    <div data-testid="firstname">{parsedValues.firstname}</div>
                    <div data-testid="lastname">{parsedValues.lastname}</div>
                    <div data-testid="postcode">{parsedValues.postcode}</div>
                    <div data-testid="region_code">{parsedValues.region_code}</div>
                    <div data-testid="save_in_address_book">{parsedValues.save_in_address_book}</div>
                    <div data-testid="street0">{parsedValues.street0}</div>
                    <div data-testid="telephone">{parsedValues.telephone}</div>
                </>
            );
        };

        const { getByTestId } = render(<Wrapper />);

        await wait(() => {
            const city = getByTestId('city');
            expect(city.textContent).toEqual('Calder');

            const default_shipping = getByTestId('default_shipping');
            expect(default_shipping.textContent).toEqual('true');

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

            const save_in_address_book = getByTestId('save_in_address_book');
            expect(save_in_address_book.textContent).toEqual('true');

            const street0 = getByTestId('street0');
            expect(street0.textContent).toEqual('address street');

            const telephone = getByTestId('telephone');
            expect(telephone.textContent).toEqual('(555) 229-3326');
        });
    });
});
