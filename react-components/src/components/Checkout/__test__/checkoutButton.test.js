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
import { fireEvent } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import CheckoutButton from '../checkoutButton';

describe('<CheckoutButton />', () => {
    it('renders the checkout button', () => {
        const mockOnClick = jest.fn(() => {});
        const { asFragment } = render(<CheckoutButton disabled={false} onClick={mockOnClick} />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('calls the onclick handler', () => {
        const mockOnClick = jest.fn(() => {});
        const { getByRole } = render(<CheckoutButton disabled={false} onClick={mockOnClick} />);

        fireEvent.click(getByRole('button'));
        expect(mockOnClick.mock.calls.length).toBeGreaterThan(0);
    });
});
