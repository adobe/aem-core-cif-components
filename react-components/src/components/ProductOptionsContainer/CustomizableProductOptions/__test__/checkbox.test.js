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
import Checkbox from '../checkbox';
import { fireEvent } from '@testing-library/react';
import { render } from 'test-utils';

describe('<Checkbox>', () => {
    const option_id = 1;
    const required = true;

    const sortedOptions = [
        {
            id: 1,
            price: 12,
            label: 'Carmina Necklace'
        },
        {
            id: 2,
            price: 13,
            label: 'Augusta Necklace'
        }
    ];

    const customization = [
        {
            id: 1,
            price: 12,
            label: 'Carmina Necklace'
        }
    ];

    const handleSelectionChange = jest.fn();

    it('renders the component', () => {
        const { asFragment } = render(
            <Checkbox
                option_id={option_id}
                required={required}
                currencyCode="USD"
                options={sortedOptions}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('tests selection change', () => {
        const { getByRole } = render(
            <Checkbox
                option_id={option_id}
                required={required}
                currencyCode="USD"
                options={sortedOptions}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        fireEvent.click(getByRole('checkbox', { name: 'Augusta Necklace + $13.00' }));

        const newCustomization = [
            {
                id: 1,
                price: 12,
                label: 'Carmina Necklace'
            },
            {
                id: 2,
                price: 13,
                label: 'Augusta Necklace'
            }
        ];

        expect(handleSelectionChange).toHaveBeenCalledTimes(1);
        expect(handleSelectionChange).toHaveBeenCalledWith(option_id, newCustomization);
    });
});
