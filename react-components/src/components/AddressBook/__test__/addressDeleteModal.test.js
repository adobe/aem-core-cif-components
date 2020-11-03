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
import AddressDeleteModal from '../addressDeleteModal';

// avoid console errors logged during testing
console.error = jest.fn();

describe('<AddressDeleteModal>', () => {
    it('renders the component', () => {
        const { asFragment } = render(<AddressDeleteModal />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('handle click event of confirm delete address button', async () => {
        const handler = jest.fn(state => state);

        const { getByText } = render(
            <UserContextProvider reducerFactory={() => handler}>
                <AddressDeleteModal />
            </UserContextProvider>
        );
        fireEvent.click(getByText('Delete'));
        await wait(() => {
            expect(handler.mock.calls.length).toEqual(1);
        });
    });

    it('handle click event of cancel delete address button', async () => {
        const handler = jest.fn(state => state);

        const { getByText } = render(
            <UserContextProvider reducerFactory={() => handler}>
                <AddressDeleteModal />
            </UserContextProvider>
        );
        fireEvent.click(getByText('Cancel'));
        await wait(() => {
            expect(handler.mock.calls.length).toEqual(1);
        });
    });
});
