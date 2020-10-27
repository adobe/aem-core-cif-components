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
import { CartProvider } from '../../Minicart';
import MyAccount from '../myAccount';

// avoid console errors logged during testing
console.error = jest.fn();

describe('<MyAccount>', () => {
    it('renders the component', async () => {
        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <MyAccount showMenu={jest.fn()} showAccountInformation={jest.fn()} showChangePassword={jest.fn()} />
            </CartProvider>
        );
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('renders the loading indicator when inProgress is true', async () => {
        const stateWithInProgress = { inProgress: true };

        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <MyAccount showMenu={jest.fn()} showAccountInformation={jest.fn()} showChangePassword={jest.fn()} />
            </CartProvider>,
            { userContext: stateWithInProgress }
        );
        await wait(() => {
            expect(asFragment()).toMatchSnapshot();
        });
    });

    it('call the callback function when sign out button is clicked', async () => {
        const mockReducerFactory = jest.fn(state => state);

        const { getByText } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => mockReducerFactory}>
                <MyAccount showMenu={jest.fn()} showAccountInformation={jest.fn()} showChangePassword={jest.fn()} />
            </CartProvider>
        );

        await wait(() => {
            expect(getByText('Sign Out')).not.toBeUndefined();
            fireEvent.click(getByText('Sign Out'));

            expect(mockReducerFactory).toHaveBeenCalledTimes(1);
        });
    });
});
