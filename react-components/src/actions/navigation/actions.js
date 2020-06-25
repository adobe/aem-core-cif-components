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

/*
  Views:
      SIGNIN - the signing modal is open
      MENU - the default view
      CREATE_ACCOUNT - the create account modal
      MY_ACCOUNT - the account props modal
      ACCOUNT_CREATED - the account created success message modal
      FORGOT_PASSWORD - the forgot password modal
      CHANGE_PASSWORD - the change password modal

*/

// DOM events that are used to talk to the navigation panel
const events = {
    START_ACC_MANAGEMENT: 'aem.accmg.start',
    EXIT_ACC_MANAGEMENT: 'aem.accmg.exit'
};

// a map of the UI states so we can properly handle the "BACK" button

const startAccMgEvent = new CustomEvent(events.START_ACC_MANAGEMENT);
const exitAccMgEvent = new CustomEvent(events.EXIT_ACC_MANAGEMENT);

const navigationPanel = document.querySelector('aside.navigation__root');
const dispatchEvent = event => navigationPanel && navigationPanel.dispatchEvent(event);

const stepTitles = {
    CREATE_ACCOUNT: t => t('account:create-account', 'Create account'),
    FORGOT_PASSWORD: t => t('account:password-recovery', 'Password recovery'),
    CHANGE_PASSWORD: t => t('account:change-password', 'Change Password'),
    CUSTOMER_ORDERS: t => t('account:customer-orders', 'Customer Orders'),
    MY_ACCOUNT: t => t('account:my-account', 'My account'),
    ACCOUNT_CREATED: t => t('account:account-created', 'Account created'),
    SIGN_IN: t => t('account:sign-in', 'Sign In')
};

export const showSignIn = ({ dispatch, t }) => {
    const view = 'SIGN_IN';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};

export const showMenu = ({ dispatch, t }) => {
    dispatch({ type: 'changeView', view: 'MENU' });
    dispatchEvent(exitAccMgEvent);
};

export const showMyAccount = ({ dispatch, t }) => {
    const view = 'MY_ACCOUNT';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};

export const showChangePassword = ({ dispatch, t }) => {
    const view = 'CHANGE_PASSWORD';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};

export const showCustomerOrders = ({ dispatch, t }) => {
    const view = 'CUSTOMER_ORDERS';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};

export const showForgotPassword = ({ dispatch, t }) => {
    const view = 'FORGOT_PASSWORD';
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};

export const showCreateAccount = ({ dispatch, t }) => {
    const view = 'CREATE_ACCOUNT';
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};

export const showAccountCreated = ({ dispatch, t }) => {
    const view = 'ACCOUNT_CREATED';
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](t) } }));
    dispatch({ type: 'changeView', view });
};
