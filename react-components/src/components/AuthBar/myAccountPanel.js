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

import SignIn from '../SignIn';
import MyAccount from '../MyAccount';
import ForgotPassword from '../ForgotPassword';
import CreateAccount, { CreateAccountSuccess } from '../CreateAccount';
import ChangePassword from '../ChangePassword';

import classes from './myAccountPanel.css';
import { string, shape, func } from 'prop-types';

const AuthModal = ({ view = 'MENU', api, children }) => {
    const {
        showSignIn,
        showMyAccount,
        showMenu,
        showForgotPassword,
        showChangePassword,
        showCreateAccount,
        showAccountCreated,
        handleBack
    } = api;

    let child;

    switch (view) {
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
            child = (
                <MyAccount
                    showMenu={showMenu}
                    showChangePassword={showChangePassword}
                    showAccountInformation={() => {}}>
                    {children}
                </MyAccount>
            );
            break;
        case 'CHANGE_PASSWORD':
            child = <ChangePassword showMyAccount={showMyAccount} handleCancel={handleBack} />;
            break;
        case 'FORGOT_PASSWORD':
            child = <ForgotPassword onClose={showMenu} onCancel={handleBack} />;
            break;
        case 'CREATE_ACCOUNT':
            child = (
                <CreateAccount
                    showMyAccount={showMyAccount}
                    showAccountCreated={showAccountCreated}
                    handleCancel={handleBack}
                />
            );
            break;
        case 'ACCOUNT_CREATED':
            child = <CreateAccountSuccess showSignIn={showSignIn} />;
    }

    return <div className={classes.root}>{child}</div>;
};

AuthModal.propTypes = {
    view: string,
    api: shape({
        showSignIn: func,
        showMyAccount: func,
        showMenu: func,
        showForgotPassword: func,
        showChangePassword: func,
        showCreateAccount: func,
        showAccountCreated: func,
        handleBack: func
    })
};

export default AuthModal;
