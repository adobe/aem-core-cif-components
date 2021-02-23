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
import { fireEvent, waitForElement } from '@testing-library/react';
import { render } from 'test-utils';
import { useUserContext } from '../UserContext';
import { useAwaitQuery } from '../../utils/hooks';

import QUERY_CUSTOMER_CART from '../../queries/query_customer_cart.graphql';

describe('UserContext test', () => {
    beforeEach(() => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: ''
        });
    });

    it('updates the user token in state', async () => {
        const ContextWrapper = () => {
            const [{ token, isSignedIn }, { setToken }] = useUserContext();

            let content;
            if (isSignedIn) {
                content = <div data-testid="success">{token}</div>;
            } else {
                content = <button onClick={() => setToken('guest123')}>Sign in</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('guest123');
    });

    it('updates the cart id of the user', async () => {
        const ContextWrapper = () => {
            const [{ cartId }, { setCustomerCart }] = useUserContext();

            let content;
            if (cartId) {
                content = <div data-testid="success">{cartId}</div>;
            } else {
                content = <button onClick={() => setCustomerCart('guest123')}>Update cart id</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('guest123');
    });

    it('resets the customer cart', async () => {
        const ContextWrapper = () => {
            const [{ cartId }, { resetCustomerCart }] = useUserContext();
            const fetchCustomerCartQuery = useAwaitQuery(QUERY_CUSTOMER_CART);

            let content;
            if (cartId) {
                content = <div data-testid="success">{cartId}</div>;
            } else {
                content = <button onClick={() => resetCustomerCart(fetchCustomerCartQuery)}>Reset cart</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('customercart');
    });

    it('performs a sign out', async () => {
        const ContextWrapper = () => {
            const [{ isSignedIn }, { signOut }] = useUserContext();
            let content;
            if (isSignedIn) {
                content = (
                    <div>
                        <span>Signed in</span>
                        <button onClick={() => signOut()}>{'Sign out'}</button>
                    </div>
                );
            } else {
                content = <div data-testid="success">{'Signed out'}</div>;
            }

            return <div>{content}</div>;
        };

        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.userToken=token123;'
        });

        const { getByRole, getByTestId, getByText } = render(<ContextWrapper />);

        expect(getByText('Signed in')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));

        const result = await waitForElement(() => getByTestId('success'));
        expect(result).not.toBeUndefined();

        // normally the browser just removes a cookie with Max-Age=0
        //...but we're not in a browser
        const expectedCookieValue = 'cif.userToken=;path=/; domain=localhost;Max-Age=0';
        expect(document.cookie).toEqual(expectedCookieValue);
    });

    it('opens account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ isAccountDropdownOpen }, { toggleAccountDropdown }] = useUserContext();

            let content;
            if (isAccountDropdownOpen) {
                content = <div data-testid="account-dropdown-open">Account dropdown opened</div>;
            } else {
                content = <button onClick={() => toggleAccountDropdown(true)}>Open account dropdown</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('account-dropdown-open'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Account dropdown opened');
    });

    it('shows sign in and forgot password views in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showForgotPassword, showSignIn }] = useUserContext();

            let content;
            if (accountDropdownView === 'SIGN_IN' || accountDropdownView === null) {
                content = (
                    <>
                        <div data-testid="sign-in-view">Sign-in view shown</div>
                        <button onClick={() => showForgotPassword()}>Show forgot password view</button>
                    </>
                );
            } else {
                content = (
                    <>
                        <div data-testid="forgot-password-view">Forgot password view shown</div>;
                        <button data-testid="show-sign-in-button" onClick={() => showSignIn()}>
                            Show sign-in view
                        </button>
                    </>
                );
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const forgotPasswordView = await waitForElement(() => getByTestId('forgot-password-view'));
        expect(forgotPasswordView).not.toBeUndefined();
        expect(forgotPasswordView.textContent).toEqual('Forgot password view shown');

        const showSignInButton = getByTestId('show-sign-in-button');
        expect(showSignInButton).not.toBeUndefined();
        fireEvent.click(showSignInButton);

        const result = await waitForElement(() => getByTestId('sign-in-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Sign-in view shown');
    });

    it('shows create account view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showCreateAccount }] = useUserContext();

            let content;
            if (accountDropdownView === 'CREATE_ACCOUNT') {
                content = <div data-testid="create-account-view">Create account view shown</div>;
            } else {
                content = <button onClick={() => showCreateAccount()}>Show create account view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('create-account-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Create account view shown');
    });

    it('shows my account view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showMyAccount }] = useUserContext();

            let content;
            if (accountDropdownView === 'MY_ACCOUNT') {
                content = <div data-testid="my-account-view">My account view shown</div>;
            } else {
                content = <button onClick={() => showMyAccount()}>Show my account view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('my-account-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('My account view shown');
    });

    it('shows account created view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showAccountCreated }] = useUserContext();

            let content;
            if (accountDropdownView === 'ACCOUNT_CREATED') {
                content = <div data-testid="account-created-view">Account created view shown</div>;
            } else {
                content = <button onClick={() => showAccountCreated()}>Show account created view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('account-created-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Account created view shown');
    });

    it('shows change password view in account dropdown', async () => {
        const ContextWrapper = () => {
            const [{ accountDropdownView }, { showChangePassword }] = useUserContext();

            let content;
            if (accountDropdownView === 'CHANGE_PASSWORD') {
                content = <div data-testid="change-password-view">Change password view shown</div>;
            } else {
                content = <button onClick={() => showChangePassword()}>Show Change password view</button>;
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const result = await waitForElement(() => getByTestId('change-password-view'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('Change password view shown');
    });

    it('performs create a user address', async () => {
        const ContextWrapper = () => {
            const [{ currentUser, addressFormError, isShowAddressForm }, { dispatch }] = useUserContext();

            let content;
            if (currentUser.addresses.length > 0) {
                content = (
                    <>
                        <div data-testid="current-user-firstname">{currentUser.firstname}</div>;
                        <div data-testid="current-user-lastname">{currentUser.lastname}</div>;
                        <div data-testid="current-user-email">{currentUser.email}</div>;
                        <div data-testid="user-address-id">{currentUser.addresses[0].id}</div>
                        <div data-testid="user-address-firstname">{currentUser.addresses[0].firstname}</div>
                        <div data-testid="user-address-lastname">{currentUser.addresses[0].lastname}</div>
                        <div data-testid="user-address-street">{currentUser.addresses[0].street}</div>
                        <div data-testid="address-form-error">{addressFormError}</div>
                        <div data-testid="is-show-address-form">{isShowAddressForm.toString()}</div>
                    </>
                );
            } else {
                content = (
                    <button
                        onClick={() =>
                            dispatch({
                                type: 'postCreateAddress',
                                address: {
                                    id: 'my-address-id',
                                    firstname: 'my-address-firstname',
                                    lastname: 'my-address-lastname',
                                    street: 'my-address-street'
                                }
                            })
                        }>
                        Create a user address
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const mockInitialState = {
            addressFormError: 'address form error',
            currentUser: {
                firstname: 'current-user-firstname',
                lastname: 'current-user-lastname',
                email: 'current-user-email',
                addresses: []
            },
            isShowAddressForm: true
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />, { userContext: mockInitialState });

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const currentUserFirstname = await waitForElement(() => getByTestId('current-user-firstname'));
        expect(currentUserFirstname).not.toBeUndefined();
        expect(currentUserFirstname.textContent).toEqual('current-user-firstname');

        const currentUserLastname = getByTestId('current-user-lastname');
        expect(currentUserLastname).not.toBeUndefined();
        expect(currentUserLastname.textContent).toEqual('current-user-lastname');

        const currentUserEmail = getByTestId('current-user-email');
        expect(currentUserEmail).not.toBeUndefined();
        expect(currentUserEmail.textContent).toEqual('current-user-email');

        const userAddressId = getByTestId('user-address-id');
        expect(userAddressId).not.toBeUndefined();
        expect(userAddressId.textContent).toEqual('my-address-id');

        const userAddressFirstname = getByTestId('user-address-firstname');
        expect(userAddressFirstname).not.toBeUndefined();
        expect(userAddressFirstname.textContent).toEqual('my-address-firstname');

        const userAddressLastname = getByTestId('user-address-lastname');
        expect(userAddressLastname).not.toBeUndefined();
        expect(userAddressLastname.textContent).toEqual('my-address-lastname');

        const userAddressStreet = getByTestId('user-address-street');
        expect(userAddressStreet).not.toBeUndefined();
        expect(userAddressStreet.textContent).toEqual('my-address-street');

        const addressFormError = getByTestId('address-form-error');
        expect(addressFormError).not.toBeUndefined();
        expect(addressFormError.textContent).toEqual('');

        const isShowAddressForm = getByTestId('is-show-address-form');
        expect(isShowAddressForm).not.toBeUndefined();
        expect(isShowAddressForm.textContent).toEqual('false');
    });

    it('performs edit an address, including begin edit, end edit, and confirm edit', async () => {
        const mockAddress = {
            id: 'my-address-id',
            firstname: 'my-address-firstname',
            lastname: 'my-address-lastname',
            street: 'my-address-street'
        };

        const ContextWrapper = () => {
            const [
                { updateAddress, isShowAddressForm, currentUser, addressFormError },
                { dispatch }
            ] = useUserContext();

            let content;
            if (updateAddress) {
                content = (
                    <>
                        <div data-testid="is-show-address-form">{isShowAddressForm.toString()}</div>
                        <button
                            data-testid="post-update-address-button"
                            onClick={() =>
                                dispatch({
                                    type: 'postUpdateAddress',
                                    address: {
                                        ...mockAddress,
                                        firstname: 'update-address-firstname',
                                        lastname: 'update-address-lastname',
                                        street: 'update-address-street'
                                    }
                                })
                            }>
                            Save address
                        </button>
                        <button
                            data-testid="end-editing-address-button"
                            onClick={() => dispatch({ type: 'endEditingAddress' })}>
                            Cancel edit
                        </button>
                    </>
                );
            } else {
                content = (
                    <>
                        <div data-testid="updated-address-firstname">{currentUser.addresses[0].firstname}</div>
                        <div data-testid="updated-address-lastname">{currentUser.addresses[0].lastname}</div>
                        <div data-testid="updated-address-street">{currentUser.addresses[0].street}</div>
                        <div data-testid="user-address-firstname">{currentUser.addresses[1].firstname}</div>
                        <div data-testid="user-address-lastname">{currentUser.addresses[1].lastname}</div>
                        <div data-testid="user-address-street">{currentUser.addresses[1].street}</div>
                        <div data-testid="address-form-error">{addressFormError}</div>
                        <button
                            data-testid="begin-editing-address-button"
                            onClick={() =>
                                dispatch({
                                    type: 'beginEditingAddress',
                                    address: mockAddress
                                })
                            }>
                            Edit address
                        </button>
                    </>
                );
            }

            return <div>{content}</div>;
        };

        const mockInitialState = {
            addressFormError: 'address form error',
            currentUser: {
                addresses: [mockAddress, { ...mockAddress, id: 'address-id' }]
            },
            isShowAddressForm: false
        };

        const { getByTestId } = render(<ContextWrapper />, { userContext: mockInitialState });

        expect(getByTestId('begin-editing-address-button')).not.toBeUndefined();
        fireEvent.click(getByTestId('begin-editing-address-button'));

        const isShowAddressForm = await waitForElement(() => getByTestId('is-show-address-form'));
        expect(isShowAddressForm).not.toBeUndefined();
        expect(isShowAddressForm.textContent).toEqual('true');

        const endEditingAddressButton = getByTestId('end-editing-address-button');
        expect(endEditingAddressButton).not.toBeUndefined();
        fireEvent.click(endEditingAddressButton);

        const beginEditingAddressButton = await waitForElement(() => getByTestId('begin-editing-address-button'));
        expect(beginEditingAddressButton).not.toBeUndefined();
        fireEvent.click(beginEditingAddressButton);

        const postUpdateAddressButton = getByTestId('post-update-address-button');
        expect(postUpdateAddressButton).not.toBeUndefined();
        fireEvent.click(postUpdateAddressButton);

        const updatedAddressFirstname = getByTestId('updated-address-firstname');
        expect(updatedAddressFirstname).not.toBeUndefined();
        expect(updatedAddressFirstname.textContent).toEqual('update-address-firstname');

        const updatedAddressLastname = getByTestId('updated-address-lastname');
        expect(updatedAddressLastname).not.toBeUndefined();
        expect(updatedAddressLastname.textContent).toEqual('update-address-lastname');

        const updatedAddressStreet = getByTestId('updated-address-street');
        expect(updatedAddressStreet).not.toBeUndefined();
        expect(updatedAddressStreet.textContent).toEqual('update-address-street');

        const userAddressFirstname = getByTestId('user-address-firstname');
        expect(userAddressFirstname).not.toBeUndefined();
        expect(userAddressFirstname.textContent).toEqual('my-address-firstname');

        const userAddressLastname = getByTestId('user-address-lastname');
        expect(userAddressLastname).not.toBeUndefined();
        expect(userAddressLastname.textContent).toEqual('my-address-lastname');

        const userAddressStreet = getByTestId('user-address-street');
        expect(userAddressStreet).not.toBeUndefined();
        expect(userAddressStreet.textContent).toEqual('my-address-street');

        const addressFormError = getByTestId('address-form-error');
        expect(addressFormError).not.toBeUndefined();
        expect(addressFormError.textContent).toEqual('');
    });

    it('performs delete an address, including begin delete, end delete, and confirm delete', async () => {
        const mockAddress = {
            id: 'my-address-id',
            firstname: 'my-address-firstname',
            lastname: 'my-address-lastname',
            street: 'my-address-street'
        };

        const ContextWrapper = () => {
            const [{ deleteAddress, currentUser, deleteAddressError }, { dispatch }] = useUserContext();

            let content;
            if (deleteAddress) {
                content = (
                    <>
                        <button
                            data-testid="post-delete-address-button"
                            onClick={() =>
                                dispatch({
                                    type: 'postDeleteAddress',
                                    address: mockAddress
                                })
                            }>
                            Confirm delete
                        </button>
                        <button
                            data-testid="end-delete-address-button"
                            onClick={() => dispatch({ type: 'endDeletingAddress' })}>
                            Cancel delete
                        </button>
                    </>
                );
            } else {
                content = (
                    <>
                        <div data-testid="user-addresses-length">{currentUser.addresses.length}</div>
                        <div data-testid="delete-address">{deleteAddress}</div>
                        <div data-testid="delete-address-error">{deleteAddressError}</div>
                        <button
                            data-testid="begin-deleting-address-button"
                            onClick={() =>
                                dispatch({
                                    type: 'beginDeletingAddress',
                                    address: mockAddress
                                })
                            }>
                            Delete address
                        </button>
                    </>
                );
            }

            return <div>{content}</div>;
        };

        const mockInitialState = {
            currentUser: {
                addresses: [mockAddress]
            },
            deleteAddressError: 'delete address error'
        };

        const { getByTestId } = render(<ContextWrapper />, { userContext: mockInitialState });

        expect(getByTestId('begin-deleting-address-button')).not.toBeUndefined();
        fireEvent.click(getByTestId('begin-deleting-address-button'));

        const endDeletingAddressButton = await waitForElement(() => getByTestId('end-delete-address-button'));
        expect(endDeletingAddressButton).not.toBeUndefined();
        fireEvent.click(endDeletingAddressButton);

        const deleteAddress = await waitForElement(() => getByTestId('delete-address'));
        expect(deleteAddress).not.toBeUndefined();
        expect(deleteAddress.textContent).toEqual('');

        const beginDeletingAddressButton = getByTestId('begin-deleting-address-button');
        expect(beginDeletingAddressButton).not.toBeUndefined();
        fireEvent.click(beginDeletingAddressButton);

        const postDeleteAddressButton = await waitForElement(() => getByTestId('post-delete-address-button'));
        expect(postDeleteAddressButton).not.toBeUndefined();
        fireEvent.click(postDeleteAddressButton);

        const clearedDeleteAddress = getByTestId('delete-address');
        expect(clearedDeleteAddress).not.toBeUndefined();
        expect(clearedDeleteAddress.textContent).toEqual('');

        const userAddressesLength = await waitForElement(() => getByTestId('user-addresses-length'));
        expect(userAddressesLength).not.toBeUndefined();
        expect(userAddressesLength.textContent).toEqual('0');

        const deleteAddressError = getByTestId('delete-address-error');
        expect(deleteAddressError).not.toBeUndefined();
        expect(deleteAddressError.textContent).toEqual('');
    });

    it('sets address form error and clear it from state', async () => {
        const ContextWrapper = () => {
            const [{ addressFormError }, { dispatch }] = useUserContext();

            let content;
            if (addressFormError) {
                content = (
                    <>
                        <div data-testid="address-form-error">{addressFormError}</div>;
                        <button
                            data-testid="clear-address-form-error-button"
                            onClick={() => dispatch({ type: 'clearAddressFormError' })}>
                            Clear address form error
                        </button>
                    </>
                );
            } else {
                content = (
                    <>
                        <div data-testid="address-form-error">{addressFormError}</div>;
                        <button
                            onClick={() =>
                                dispatch({ type: 'setAddressFormError', error: new Error('address form error') })
                            }>
                            Set address form error
                        </button>
                    </>
                );
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();

        fireEvent.click(getByRole('button'));
        const addressFormError = await waitForElement(() => getByTestId('address-form-error'));
        expect(addressFormError).not.toBeUndefined();
        expect(addressFormError.textContent).toEqual('address form error');

        const clearAddressFormErrorButton = getByTestId('clear-address-form-error-button');
        expect(clearAddressFormErrorButton).not.toBeUndefined();
        fireEvent.click(clearAddressFormErrorButton);

        const result = await waitForElement(() => getByTestId('address-form-error'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('');
    });

    it('sets delete address error', async () => {
        const ContextWrapper = () => {
            const [{ deleteAddressError }, { dispatch }] = useUserContext();

            let content;
            if (deleteAddressError) {
                content = <div data-testid="delete-address-error">{deleteAddressError}</div>;
            } else {
                content = (
                    <button
                        onClick={() =>
                            dispatch({ type: 'deleteAddressError', error: new Error('delete address error') })
                        }>
                        Set delete address error
                    </button>
                );
            }

            return <div>{content}</div>;
        };

        const { getByRole, getByTestId } = render(<ContextWrapper />);

        expect(getByRole('button')).not.toBeUndefined();
        fireEvent.click(getByRole('button'));

        const result = await waitForElement(() => getByTestId('delete-address-error'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('delete address error');
    });

    it('opens address form and closes address form', async () => {
        const ContextWrapper = () => {
            const [{ isShowAddressForm }, { dispatch }] = useUserContext();

            let content;
            if (isShowAddressForm) {
                content = (
                    <>
                        <div data-testid="is-show-address-form">{isShowAddressForm.toString()}</div>;
                        <button
                            data-testid="close-address-form-button"
                            onClick={() => dispatch({ type: 'closeAddressForm' })}>
                            Close address form
                        </button>
                    </>
                );
            } else {
                content = (
                    <>
                        <div data-testid="is-show-address-form">{isShowAddressForm.toString()}</div>;
                        <button
                            data-testid="open-address-form-button"
                            onClick={() => dispatch({ type: 'openAddressForm' })}>
                            Open address form
                        </button>
                    </>
                );
            }

            return <div>{content}</div>;
        };

        const { getByTestId } = render(<ContextWrapper />);

        expect(getByTestId('open-address-form-button')).not.toBeUndefined();
        fireEvent.click(getByTestId('open-address-form-button'));

        const isShowAddressForm = await waitForElement(() => getByTestId('is-show-address-form'));
        expect(isShowAddressForm).not.toBeUndefined();
        expect(isShowAddressForm.textContent).toEqual('true');

        expect(getByTestId('close-address-form-button')).not.toBeUndefined();
        fireEvent.click(getByTestId('close-address-form-button'));

        const result = await waitForElement(() => getByTestId('is-show-address-form'));
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('false');
    });
});
