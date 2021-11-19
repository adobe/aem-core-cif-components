/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
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

// required for the bable plugin to pickup the messages in this file
import 'react-intl';

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
    CREATE_ACCOUNT: intl => intl.formateMessage({ id: 'account:create-account', defaultMessage: 'Create account' }),
    FORGOT_PASSWORD: intl =>
        intl.formateMessage({ id: 'account:password-recovery', defaultMessage: 'Password recovery' }),
    CHANGE_PASSWORD: intl => intl.formateMessage({ id: 'account:change-password', defaultMessage: 'Change Password' }),
    MY_ACCOUNT: intl => intl.formateMessage({ id: 'account:my-account', defaultMessage: 'My account' }),
    ACCOUNT_CREATED: intl => intl.formateMessage({ id: 'account:account-created', defaultMessage: 'Account created' }),
    SIGN_IN: intl => intl.formateMessage({ id: 'account:sign-in', defaultMessage: 'Sign In' })
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showSignIn = ({ dispatch, intl }) => {
    const view = 'SIGN_IN';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](intl) } }));
    dispatch({ type: 'changeView', view });
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showMenu = ({ dispatch }) => {
    dispatch({ type: 'changeView', view: 'MENU' });
    dispatchEvent(exitAccMgEvent);
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showMyAccount = ({ dispatch, intl }) => {
    const view = 'MY_ACCOUNT';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](intl) } }));
    dispatch({ type: 'changeView', view });
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showChangePassword = ({ dispatch, intl }) => {
    const view = 'CHANGE_PASSWORD';
    dispatchEvent(startAccMgEvent);
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](intl) } }));
    dispatch({ type: 'changeView', view });
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showForgotPassword = ({ dispatch, intl }) => {
    const view = 'FORGOT_PASSWORD';
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](intl) } }));
    dispatch({ type: 'changeView', view });
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showCreateAccount = ({ dispatch, intl }) => {
    const view = 'CREATE_ACCOUNT';
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](intl) } }));
    dispatch({ type: 'changeView', view });
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showAccountCreated = ({ dispatch, intl }) => {
    const view = 'ACCOUNT_CREATED';
    dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view](intl) } }));
    dispatch({ type: 'changeView', view });
};

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
export const showView = ({ dispatch, intl, view }) => {
    const title = stepTitles[view];
    if (title) {
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: title(intl) } }));
        dispatch({ type: 'changeView', view });
    }
};
