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
import TextField from '../textfield';
import { fireEvent } from '@testing-library/react';
import { render } from 'test-utils';

describe('<Radio>', () => {
    const option_id = 1;
    const required = true;

    const value = {
        max_characters: 20,
        price: 3,
        price_type: 'FIXED'
    };

    const customization = '';

    const handleSelectionChange = jest.fn();

    beforeEach(() => {
        handleSelectionChange.mockClear();
    });

    it('renders the input component', () => {
        const { asFragment } = render(
            <TextField
                option_id={option_id}
                required={required}
                currencyCode="USD"
                max_characters={value.max_characters}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the textarea component', () => {
        const { asFragment } = render(
            <TextField
                option_id={option_id}
                required={required}
                currencyCode="USD"
                textarea
                max_characters={value.max_characters}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('tests input change', () => {
        const { getByRole } = render(
            <TextField
                option_id={option_id}
                required={required}
                currencyCode="USD"
                max_characters={value.max_characters}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        const newCustomization = '23';
        const input = getByRole('textbox');
        fireEvent.change(input, { target: { value: newCustomization } });

        expect(handleSelectionChange).toHaveBeenCalledTimes(1);
        expect(handleSelectionChange).toHaveBeenCalledWith(option_id, newCustomization);
    });

    it('tests input character limit', () => {
        const { getByRole } = render(
            <TextField
                option_id={option_id}
                required={required}
                currencyCode="USD"
                max_characters={value.max_characters}
                customization={customization}
                handleSelectionChange={handleSelectionChange}
            />
        );

        const inputText = 'a'.repeat(21);
        const expectedText = 'a'.repeat(20);
        const input = getByRole('textbox');
        fireEvent.change(input, { target: { value: inputText } });

        expect(handleSelectionChange).toHaveBeenCalledTimes(1);
        expect(handleSelectionChange).toHaveBeenCalledWith(option_id, expectedText);
    });
});
