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
import { render, fireEvent, waitForElement } from '@testing-library/react';
import { MockedProvider } from '@apollo/react-testing';
import NavigationContextProvider, { useNavigationContext } from '../NavigationContext';

describe('NavigationContext test', () => {
    const ContextWrapper = () => {
        const [
            { view },
            { showSignIn, showMyAccount, showForgotPassword, showChangePassword, showCreateAccount, showAccountCreated }
        ] = useNavigationContext();

        let child;
        let content;

        switch (view) {
            case 'MENU':
                child = <div data-testid="menu">Menu page</div>;
                break;
            case 'SIGN_IN':
                child = <div data-testid="sign_in">Sign in page</div>;
                break;
            case 'MY_ACCOUNT':
                child = <div data-testid="my_account">My account page</div>;
                break;
            case 'CHANGE_PASSWORD':
                child = <div data-testid="change_password">Change password page</div>;
                break;
            case 'FORGOT_PASSWORD':
                child = <div data-testid="forgot_password">Forgot password page</div>;
                break;
            case 'CREATE_ACCOUNT':
                child = <div data-testid="create_account">Create account page</div>;
                break;
            case 'ACCOUNT_CREATED':
                child = <div data-testid="account_created">Account created page</div>;
                break;
        }

        content = (
            <>
                <div>{child}</div>
                <button onClick={showSignIn}>Sign in</button>
                <button onClick={showMyAccount}>My account</button>
                <button onClick={showForgotPassword}>Forgot password</button>
                <button onClick={showChangePassword}>Change password</button>
                <button onClick={showCreateAccount}>Create account</button>
                <button onClick={showAccountCreated}>Account created</button>
            </>
        );

        return <div>{content}</div>;
    };

    it('shows sign in page', async () => {
        const { getByText, getByTestId } = render(
            <MockedProvider addTypename={false}>
                <NavigationContextProvider>
                    <ContextWrapper />
                </NavigationContextProvider>
            </MockedProvider>
        );

        expect(getByText('Sign in')).not.toBeUndefined();

        fireEvent.click(getByText('Sign in'));
        const result = await waitForElement(() => getByTestId('sign_in'));
        expect(result.textContent).toEqual('Sign in page');

        fireEvent(document, new CustomEvent('aem.navigation.back'));

        const backResult = await waitForElement(() => getByTestId('menu'));
        expect(backResult.textContent).toEqual('Menu page');
    });

    it('shows my account page', async () => {
        const { getByText, getByTestId } = render(
            <MockedProvider addTypename={false}>
                <NavigationContextProvider>
                    <ContextWrapper />
                </NavigationContextProvider>
            </MockedProvider>
        );

        expect(getByText('My account')).not.toBeUndefined();

        fireEvent.click(getByText('My account'));
        const result = await waitForElement(() => getByTestId('my_account'));
        expect(result.textContent).toEqual('My account page');

        fireEvent(document, new CustomEvent('aem.navigation.back'));

        const backResult = await waitForElement(() => getByTestId('menu'));
        expect(backResult.textContent).toEqual('Menu page');
    });

    it('shows forgot password page', async () => {
        const { getByText, getByTestId } = render(
            <MockedProvider addTypename={false}>
                <NavigationContextProvider>
                    <ContextWrapper />
                </NavigationContextProvider>
            </MockedProvider>
        );

        expect(getByText('Forgot password')).not.toBeUndefined();

        fireEvent.click(getByText('Forgot password'));
        const result = await waitForElement(() => getByTestId('forgot_password'));
        expect(result.textContent).toEqual('Forgot password page');

        fireEvent(document, new CustomEvent('aem.navigation.back'));

        const backResult = await waitForElement(() => getByTestId('sign_in'));
        expect(backResult.textContent).toEqual('Sign in page');
    });

    it('shows change password page', async () => {
        const { getByText, getByTestId } = render(
            <MockedProvider addTypename={false}>
                <NavigationContextProvider>
                    <ContextWrapper />
                </NavigationContextProvider>
            </MockedProvider>
        );

        expect(getByText('Change password')).not.toBeUndefined();

        fireEvent.click(getByText('Change password'));
        const result = await waitForElement(() => getByTestId('change_password'));
        expect(result.textContent).toEqual('Change password page');

        fireEvent(document, new CustomEvent('aem.navigation.back'));

        const backResult = await waitForElement(() => getByTestId('my_account'));
        expect(backResult.textContent).toEqual('My account page');
    });

    it('shows create account page', async () => {
        const { getByText, getByTestId } = render(
            <MockedProvider addTypename={false}>
                <NavigationContextProvider>
                    <ContextWrapper />
                </NavigationContextProvider>
            </MockedProvider>
        );

        expect(getByText('Create account')).not.toBeUndefined();

        fireEvent.click(getByText('Create account'));
        const result = await waitForElement(() => getByTestId('create_account'));
        expect(result.textContent).toEqual('Create account page');

        fireEvent(document, new CustomEvent('aem.navigation.back'));

        const backResult = await waitForElement(() => getByTestId('sign_in'));
        expect(backResult.textContent).toEqual('Sign in page');
    });

    it('shows account created page', async () => {
        const { getByText, getByTestId } = render(
            <MockedProvider addTypename={false}>
                <NavigationContextProvider>
                    <ContextWrapper />
                </NavigationContextProvider>
            </MockedProvider>
        );

        expect(getByText('Account created')).not.toBeUndefined();

        fireEvent.click(getByText('Account created'));
        const result = await waitForElement(() => getByTestId('account_created'));
        expect(result.textContent).toEqual('Account created page');

        fireEvent(document, new CustomEvent('aem.navigation.back'));

        const backResult = await waitForElement(() => getByTestId('menu'));
        expect(backResult.textContent).toEqual('Menu page');
    });
});
