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
import { screen, fireEvent, wait } from '@testing-library/react';
import { render } from 'test-utils';
import { CheckoutProvider } from '../../Checkout';
import AddressForm from '../addressForm';

describe('<AddressForm />', () => {
    const countries = [
        {
            id: 'US',
            available_regions: [
                {
                    name: 'Michigan',
                    code: 'MI'
                }
            ]
        }
    ];

    it('renders the component', async () => {
        const { asFragment } = render(<AddressForm cancel={() => {}} submit={() => {}} />);
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('renders the component with address validation message and form error message', async () => {
        const { asFragment } = render(
            <AddressForm
                isAddressInvalid={true}
                validationMessage={'address validation message'}
                formErrorMessage={'form error message'}
                cancel={() => {}}
                submit={() => {}}
            />
        );
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('renders the component with address select', async () => {
        const { asFragment } = render(
            <CheckoutProvider>
                <AddressForm
                    showAddressSelect={true}
                    initialAddressSelectValue={0}
                    onAddressSelectValueChange={() => {}}
                    cancel={() => {}}
                    submit={() => {}}
                />
            </CheckoutProvider>
        );
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('renders the component with default address checkbox', async () => {
        const { asFragment } = render(
            <CheckoutProvider>
                <AddressForm showDefaultAddressCheckbox={true} cancel={() => {}} submit={() => {}} />
            </CheckoutProvider>
        );
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('renders the component with save in address book checkbox', async () => {
        const { asFragment } = render(
            <CheckoutProvider>
                <AddressForm showSaveInAddressBookCheckbox={true} cancel={() => {}} submit={() => {}} />
            </CheckoutProvider>
        );
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('calls the callback function when changing the address select', async () => {
        const onAddressSelectValueChange = jest.fn(() => {});
        render(
            <CheckoutProvider>
                <AddressForm
                    showAddressSelect={true}
                    initialAddressSelectValue={0}
                    onAddressSelectValueChange={onAddressSelectValueChange}
                    cancel={() => {}}
                    submit={() => {}}
                />
            </CheckoutProvider>
        );
        const addressSelect = screen.getByDisplayValue('New Address');
        expect(addressSelect).not.toBeUndefined();
        fireEvent.change(addressSelect, { target: { value: '1' } });
        await wait(() => {
            expect(onAddressSelectValueChange).toHaveBeenCalledTimes(1);
        });
    });

    it('fills the street0 field with a given street array value', async () => {
        const initialValues = {
            street: ['street A', 'street B']
        };

        const { container } = render(<AddressForm cancel={() => {}} submit={() => {}} initialValues={initialValues} />);

        await wait(() => {
            const streetField = container.querySelector('#street0');
            expect(streetField.value).toEqual('street A');
        });
    });

    it('returns the street field as array', async () => {
        const initialValues = {
            country_id: 'US',
            firstname: 'Veronica',
            lastname: 'Costello',
            city: 'Calder',
            postcode: '49628-7978',
            region_code: 'MI',
            region: {
                region_code: 'MI'
            },
            telephone: '(555) 229-3326',
            email: 'veronica@example.com'
        };

        const mockSubmit = jest.fn(() => {});
        const { container } = render(
            <AddressForm cancel={() => {}} submit={mockSubmit} initialValues={initialValues} countries={countries} />
        );

        // Fill street input
        const streetField = container.querySelector('#street0');
        fireEvent.change(streetField, { target: { value: 'street A' } });

        // Click on submit
        let submit = container.querySelector('button[type=submit]');
        fireEvent.click(submit);

        // Verify parameter
        await wait(() => {
            expect(mockSubmit).toHaveBeenCalledTimes(1);
            const submitValues = mockSubmit.mock.calls[0][0];
            expect(submitValues.street).toEqual(['street A']);
        });
    });
});
