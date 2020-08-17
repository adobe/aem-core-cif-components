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
        { isAccountDropdownOpen, accountDropdownView },
        { showSignIn, showMyAccount, showForgotPassword, showCreateAccount, showAccountCreated, showChangePassword }
    ] = useUserContext();

    const [t] = useTranslation('account');

    const className = isAccountDropdownOpen ? classes.dropdown_open : classes.dropdown;

    let child;

    switch (accountDropdownView) {
        case 'SIGN_IN':
            child = (
                <SignIn
                    showMyAccount={showMyAccount}
                    showForgotPassword={showForgotPassword}
                    showCreateAccount={showCreateAccount}
                />
            );
            break;
        case 'MY_ACCOUNT':
            child = <MyAccount showChangePassword={showChangePassword} showAccountInformation={() => {}} />;
            break;
        case 'CHANGE_PASSWORD':
            child = <ChangePassword showMyAccount={showMyAccount} handleCancel={showMyAccount} />;
            break;
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
    }

    return (
        <div className={className} aria-label="account dropdown">
            {child}
        </div>
    );
};

export default AccountDropdown;
