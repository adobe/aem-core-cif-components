/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
import React from 'react';
import MultiSelect from '../multiSelect';
import { fireEvent } from '@testing-library/react';
import { render } from '../../../utils/test-utils';

describe('<MultiSelect>', () => {
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
            can_change_quantity: false,
            label: 'Carmina Necklace'
        },
        {
            id: 2,
            quantity: 1,
            price: 13,
            currency: 'USD',
            can_change_quantity: true,
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
            <MultiSelect
                item={requiredItem}
                options={sortedOptions}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('tests selection change', () => {
        const { getByRole } = render(
            <MultiSelect
                item={requiredItem}
                options={sortedOptions}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        getByRole('option', { name: 'Augusta Necklace' }).selected = true;

        fireEvent.change(getByRole('listbox'));

        const newCustomization = [
            {
                id: 1,
                price: 12,
                quantity: 1
            },
            {
                id: 2,
                price: 13,
                quantity: 1
            }
        ];

        expect(handleSelectionChange).toHaveBeenCalledTimes(1);
        expect(handleSelectionChange).toHaveBeenCalledWith(requiredItem.option_id, 1, newCustomization);
    });
});
