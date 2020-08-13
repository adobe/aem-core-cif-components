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
import MultiSelect from '../multiSelect';
import { render } from '@testing-library/react';

describe('<MultiSelect>', () => {
    const requiredItem = {
        option_id: 1,
        required: true
    };

    const sortedOptions = [
        {
            id: 1,
            quantity: 1,
            position: 1,
            is_default: true,
            price: 0,
            price_type: 'FIXED',
            can_change_quantity: false,
            label: 'Carmina Necklace',
            product: { id: 1355, name: 'Carmina Necklace', sku: 'VA13-GO-NA', __typename: 'SimpleProduct' },
            __typename: 'BundleItemOption'
        },
        {
            id: 2,
            quantity: 1,
            position: 2,
            is_default: false,
            price: 0,
            price_type: 'FIXED',
            can_change_quantity: true,
            label: 'Augusta Necklace',
            product: { id: 1356, name: 'Augusta Necklace', sku: 'VA14-SI-NA', __typename: 'SimpleProduct' },
            __typename: 'BundleItemOption'
        }
    ];

    const customization = [
        {
            id: 1,
            quantity: 1
        }
    ];

    const handleSelectionChange = jest.fn();

    it('renders the component', () => {
        const { asFragment } = render(
            <MultiSelect
                item={requiredItem}
                sortedOptions={sortedOptions}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
