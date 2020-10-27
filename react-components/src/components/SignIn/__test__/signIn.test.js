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

import { useUserContext } from '../../../context/UserContext';
import { CartProvider } from '../../Minicart/cartContext';

import SignIn from '../signIn';

describe('<SignIn>', () => {
    beforeEach(() => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=guest123'
        });
    });

    it('renders the component', () => {
        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <SignIn showMyAccount={jest.fn()} showCreateAccount={jest.fn()} showForgotPassword={jest.fn()} />
            </CartProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('switch the view then the "Sign In" is successful', async () => {
        // To simulate an almost real use case of the sign in component we create a wrapper around it
        // which displays a "success" message when the user is signed in
        const SignInWrapper = () => {
            const [{ isSignedIn, cartId }] = useUserContext();

            let content;
            if (isSignedIn && cartId) {
                content = <div data-testid="success">Done</div>;
            } else {
                content = (
                    <SignIn showMyAccount={jest.fn()} showCreateAccount={jest.fn()} showForgotPassword={jest.fn()} />
                );
            }

            return <div>{content}</div>;
        };

        const { getByTestId, getByLabelText } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <SignInWrapper />
            </CartProvider>
        );

        fireEvent.change(getByLabelText(/email/i), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText(/password/i), { target: { value: 'norris' } });
        fireEvent.click(getByLabelText('submit'));
        await wait(() => {
            expect(getByTestId('success').textContent).not.toBeUndefined();
        });
    });

    it('shows an error when the sign in is not successful', async () => {
        const { getByText, getByLabelText } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <SignIn showMyAccount={jest.fn()} showForgotPassword={jest.fn()} showCreateAccount={jest.fn()} />
            </CartProvider>
        );

        fireEvent.change(getByLabelText(/email/i), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText(/password/i), { target: { value: 'wrongpassword' } });
        fireEvent.click(getByLabelText('submit'));

        await wait(() => {
            expect(getByText('Error')).not.toBeUndefined();
        });
    });
});
