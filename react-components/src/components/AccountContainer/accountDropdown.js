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
import { useTranslation } from 'react-i18next';

import { useUserContext } from '../../context/UserContext';
import SignIn from '../SignIn/signIn';
import MyAccount from '../MyAccount/myAccount';
import ChangePassword from '../ChangePassword';
import ForgotPassword from '../ForgotPassword/forgotPassword';
import CreateAccount, { CreateAccountSuccess } from '../CreateAccount';

import classes from './accountDropdown.css';

const AccountDropdown = () => {
    const [
        { isAccountDropdownOpen, isSignedIn, accountDropdownView },
        {
            showSignIn,
            showMyAccount,
            showForgotPassword,
            showCreateAccount,
            showAccountCreated,
            showChangePassword,
            showAddressBook
        }
    ] = useUserContext();

    const [t] = useTranslation('account');

    if (!isAccountDropdownOpen) {
        return <div className={classes.dropdown} aria-label="account dropdown"></div>;
    }

    let child;
    if (!isSignedIn) {
        switch (accountDropdownView) {
            case 'FORGOT_PASSWORD':
                child = <ForgotPassword onClose={showSignIn} onCancel={showSignIn} />;
                break;
            case 'CREATE_ACCOUNT':
                child = (
                    <CreateAccount
                        showMyAccount={showMyAccount}
                        showAccountCreated={showAccountCreated}
                        handleCancel={showSignIn}
                    />
                );
                break;
            case 'ACCOUNT_CREATED':
                child = <CreateAccountSuccess showSignIn={showSignIn} />;
                break;
            case 'SIGN_IN':
            default:
                child = <SignIn showForgotPassword={showForgotPassword} showCreateAccount={showCreateAccount} />;
        }
    } else {
        switch (accountDropdownView) {
            case 'CHANGE_PASSWORD':
                child = <ChangePassword showMyAccount={showMyAccount} handleCancel={showMyAccount} />;
                break;
            case 'MY_ACCOUNT':
            default:
                child = (
                    <MyAccount
                        showChangePassword={showChangePassword}
                        showAddressBook={showAddressBook}
                        showAccountInformation={() => {}}
                    />
                );
        }
    }

    return (
        <div className={classes.dropdown_open} aria-label="account dropdown">
            {child}
        </div>
    );
};

export default AccountDropdown;
