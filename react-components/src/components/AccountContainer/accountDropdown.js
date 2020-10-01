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

import { useConfigContext } from '../../context/ConfigContext';
import { useUserContext } from '../../context/UserContext';
import SignIn from '../SignIn/signIn';
import MyAccount from '../MyAccount/myAccount';
import ChangePassword from '../ChangePassword';
import ForgotPassword from '../ForgotPassword/forgotPassword';
import CreateAccount, { CreateAccountSuccess } from '../CreateAccount';

import classes from './accountDropdown.css';
import Mask from '../Mask';

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
            toggleAccountDropdown
        }
    ] = useUserContext();
    const { pagePaths } = useConfigContext();
    const rootClass = isAccountDropdownOpen ? classes.root_open : classes.root;

    let view;
    if (!isSignedIn) {
        switch (accountDropdownView) {
            case 'FORGOT_PASSWORD':
                view = <ForgotPassword onClose={showSignIn} onCancel={showSignIn} />;
                break;
            case 'CREATE_ACCOUNT':
                view = (
                    <CreateAccount
                        showMyAccount={showMyAccount}
                        showAccountCreated={showAccountCreated}
                        handleCancel={showSignIn}
                    />
                );
                break;
            case 'ACCOUNT_CREATED':
                view = <CreateAccountSuccess showSignIn={showSignIn} />;
                break;
            case 'SIGN_IN':
            default:
                view = (
                    <SignIn
                        showMyAccount={showMyAccount}
                        showForgotPassword={showForgotPassword}
                        showCreateAccount={showCreateAccount}
                    />
                );
        }
    } else {
        switch (accountDropdownView) {
            case 'CHANGE_PASSWORD':
                view = <ChangePassword showMyAccount={showMyAccount} handleCancel={showMyAccount} />;
                break;
            case 'MY_ACCOUNT':
            default:
                view = (
                    <MyAccount
                        showChangePassword={showChangePassword}
                        showAccountInformation={() => {
                            window.location.href = pagePaths.accountDetails;
                        }}
                    />
                );
        }
    }

    return (
        <>
            <Mask
                isOpen={isAccountDropdownOpen}
                onClickHandler={() => toggleAccountDropdown(!isAccountDropdownOpen)}
                customClasses={{ root_active: classes.mask_active }}
            />

            <div className={rootClass} aria-label="account dropdown">
                {view}
            </div>
        </>
    );
};

export default AccountDropdown;
