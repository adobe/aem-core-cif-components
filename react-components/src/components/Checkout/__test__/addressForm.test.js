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
import { I18nextProvider } from 'react-i18next';

import AddressForm from '../addressForm';
import i18n from '../../../../__mocks__/i18nForTests';

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

    it('renders the component', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <AddressForm cancel={() => {}} submit={() => {}} />
            </I18nextProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('fills the street0 field with a given street array value', () => {
        const initialValues = {
            street: ['street A', 'street B']
        };

        const { container } = render(<AddressForm cancel={() => {}} submit={() => {}} initialValues={initialValues} />);

        const streetField = container.querySelector('#street0');
        expect(streetField.value).toEqual('street A');
    });

    it('returns the street field as array', () => {
        const initialValues = {
            country_id: 'US',
            firstname: 'Veronica',
            lastname: 'Costello',
            city: 'Calder',
            postcode: '49628-7978',
            region_id: 33,
            region_code: 'MI',
            region: 'Michigan',
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
        expect(mockSubmit).toHaveBeenCalledTimes(1);
        const submitValues = mockSubmit.mock.calls[0][0];
        expect(submitValues.street).toEqual(['street A']);
    });
});
