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
import { string, func } from 'prop-types';

import SignIn from '../SignIn';
import MyAccount from '../MyAccount';
import ForgotPassword from '../ForgotPassword';
import CreateAccount from '../CreateAccount';
import ChangePassword from '../ChangePassword';
import UpdateCustomerBillingAddress from '../UpdateCustomerBillingAddress';
import UpdateCustomerShippingAddress from '../UpdateCustomerShippingAddress';
import OrderHistory from '../OrderHistory';

import classes from './authModal.css';

const AuthModal = props => {
    const { view, showMyAccount, showMenu, showForgotPassword, showChangePassword, showCreateAccount, showUpdateCustomerBillingAddress, showUpdateCustomerShippingAddress, showOrderHistory } = props;

    let child;

    switch (view) {
        case 'SIGN_IN':
            child = (
                <SignIn
                    showMyAccount={showMyAccount}
                    showForgotPassword={showForgotPassword}
                    showCreateAccount={showCreateAccount}
                    showOrderHistory={showOrderHistory}
                />
            );
            break;
        case 'MY_ACCOUNT':
            child = <MyAccount showMenu={showMenu} showUpdateCustomerBillingAddress={showUpdateCustomerBillingAddress} showUpdateCustomerShippingAddress={showUpdateCustomerShippingAddress} showOrderHistory={showOrderHistory} showChangePassword={showChangePassword} />;
            break;
        case 'UPDATE_BILLING_ADDRESS':
            child = <UpdateCustomerBillingAddress showMyAccount={showMyAccount} />;
            break;
        case 'UPDATE_SHIPPING_ADDRESS':
            child = <UpdateCustomerShippingAddress showMyAccount={showMyAccount} />;
            break;
        case 'ORDER_HISTORY':
            child = <OrderHistory showMyAccount={showMyAccount} />;
            break;
        case 'CHANGE_PASSWORD':
            child = <ChangePassword showMyAccount={showMyAccount} />;
            break;
        case 'FORGOT_PASSWORD':
            child = <ForgotPassword onClose={showMenu} />;
            break;
        case 'CREATE_ACCOUNT':
            child = <CreateAccount showMyAccount={showMyAccount} />;
    }

    return <div className={classes.root}>{child}</div>;
};

AuthModal.propTypes = {
    view: string.isRequired,
    showMyAccount: func.isRequired,
    showMenu: func.isRequired,
    showForgotPassword: func.isRequired,
    showChangePassword: func.isRequired,
    showCreateAccount: func.isRequired,
    showUpdateCustomerBillingAddress: func.isRequired,
    showUpdateCustomerShippingAddress: func.isRequired,
    showOrderHistory: func.isRequired
};

export default AuthModal;
