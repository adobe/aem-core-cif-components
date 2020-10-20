/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import Radio from '../radio';
import { render, fireEvent } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';

import i18n from '../../../../__mocks__/i18nForTests';

describe('<Radio>', () => {
    const requiredItem = {
        option_id: 1,
        required: true,
        quantity: 1
    };

    const sortedOptions = [
        {
            id: 1,
            quantity: 1,
            price: 12,
            currency: 'USD',
            can_change_quantity: true,
            label: 'Carmina Necklace'
        },
        {
            id: 2,
            quantity: 1,
            price: 13,
            currency: 'USD',
            can_change_quantity: false,
            label: 'Augusta Necklace'
        }
    ];

    const customization = [
        {
            id: 1,
            price: 12,
            quantity: 1
        }
    ];

    const handleSelectionChange = jest.fn();

    it('renders the component', () => {
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <Radio
                    item={requiredItem}
                    options={sortedOptions}
                    customization={customization}
                    handleSelectionChange={handleSelectionChange}
                />
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('tests selection change', () => {
        const { getByRole } = render(
            <I18nextProvider i18n={i18n}>
                <Radio
                    item={requiredItem}
                    options={sortedOptions}
                    customization={customization}
                    handleSelectionChange={handleSelectionChange}
                />
            </I18nextProvider>
        );

        fireEvent.click(getByRole('radio', { name: 'Augusta Necklace + $13.00' }));

        const newCustomization = [
            {
                id: 2,
                price: 13,
                quantity: 1
            }
        ];

        expect(handleSelectionChange).toHaveBeenCalledTimes(1);
        expect(handleSelectionChange).toHaveBeenCalledWith(requiredItem.option_id, 1, newCustomization);
    });

    it('disables quantity change', async () => {
        const quantityDisableCustomization = [
            {
                id: 2,
                price: 13,
                quantity: 1
            }
        ];
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <Radio
                    item={requiredItem}
                    options={sortedOptions}
                    customization={quantityDisableCustomization}
                    handleSelectionChange={handleSelectionChange}
                />
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
