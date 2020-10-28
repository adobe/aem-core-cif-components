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
import { render } from 'test-utils';
import UserContextProvider from '../../../context/UserContext';
import AddAddressButton from '../addAddressButton';

describe('<AddAddressButton>', () => {
    it('renders the component', () => {
        const { asFragment } = render(<AddAddressButton />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with list display type', () => {
        const { asFragment } = render(<AddAddressButton displayType={'list'} />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('handle click event of add address button', () => {
        const handler = jest.fn(state => state);

        const { getByRole } = render(
            <UserContextProvider reducerFactory={() => handler}>
                <AddAddressButton />
            </UserContextProvider>
        );
        fireEvent.click(getByRole('button'));
        expect(handler.mock.calls.length).toEqual(1);
    });
});
