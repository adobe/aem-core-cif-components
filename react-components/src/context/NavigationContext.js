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
import React, { useCallback, useContext, useReducer, Suspense } from 'react';
import { useTranslation } from 'react-i18next';
import { object } from 'prop-types';

import LoadingIndicator from '../components/LoadingIndicator';
import { useEventListener } from '../utils/hooks';

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
const ancestors = {
    CREATE_ACCOUNT: 'SIGN_IN',
    FORGOT_PASSWORD: 'SIGN_IN',
    ACCOUNT_CREATED: 'MENU',
    MY_ACCOUNT: 'MENU',
    CHANGE_PASSWORD: 'MY_ACCOUNT',
    SIGN_IN: 'MENU',
    MENU: null
};

const startAccMgEvent = new CustomEvent(events.START_ACC_MANAGEMENT);
const exitAccMgEvent = new CustomEvent(events.EXIT_ACC_MANAGEMENT);

const NavigationContext = React.createContext();

const reducerFactory = () => {
    return (state, action) => {
        switch (action.type) {
            case 'changeView': {
                return {
                    ...state,
                    view: action.view
                };
            }
            default:
                return state;
        }
    };
};

const NavigationContextProvider = props => {
    const navigationPanel = document.querySelector('aside.navigation__root');
    const dispatchEvent = event => navigationPanel && navigationPanel.dispatchEvent(event);
    const [t] = useTranslation('account');

    const stepTitles = {
        CREATE_ACCOUNT: t('account:create-account', 'Create account'),
        FORGOT_PASSWORD: t('account:password-recovery', 'Password recovery'),
        CHANGE_PASSWORD: t('account:change-password', 'Change Password'),
        MY_ACCOUNT: t('account:my-account', 'My account'),
        ACCOUNT_CREATED: t('account:account-created', 'Account created'),
        SIGN_IN: t('account:sign-in', 'Sign In')
    };

    const initialState = props.initialState || {
        view: 'MENU'
    };

    const [navigationState, dispatch] = useReducer(reducerFactory(), initialState);
    const { view } = navigationState;

    const showSignIn = () => {
        const view = 'SIGN_IN';
        dispatchEvent(startAccMgEvent);
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        dispatch({ type: 'changeView', view });
    };

    const showMenu = () => {
        dispatch({ type: 'changeView', view: 'MENU' });
        dispatchEvent(exitAccMgEvent);
    };

    const showMyAccount = () => {
        const view = 'MY_ACCOUNT';
        dispatchEvent(startAccMgEvent);
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        dispatch({ type: 'changeView', view });
    };

    const showChangePassword = () => {
        const view = 'CHANGE_PASSWORD';
        dispatchEvent(startAccMgEvent);
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        dispatch({ type: 'changeView', view });
    };

    const showForgotPassword = () => {
        const view = 'FORGOT_PASSWORD';
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        dispatch({ type: 'changeView', view });
    };

    const showCreateAccount = () => {
        const view = 'CREATE_ACCOUNT';
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        dispatch({ type: 'changeView', view });
    };

    const showAccountCreated = () => {
        const view = 'ACCOUNT_CREATED';
        dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        dispatch({ type: 'changeView', view });
    };

    const handleBack = useCallback(() => {
        if (navigationState.view === null) {
            return;
        }
        const parent = ancestors[view];
        if (parent === 'MENU') {
            // no parent view means we're out of the account management process and back to navigation
            // so we're resetting the title
            showMenu();
            return;
        }
        if (parent === 'MY_ACCOUNT') {
            showMyAccount();
            return;
        }
        dispatch({ type: 'changeView', view: parent });
    }, [view]);

    useEventListener(document, 'aem.navigation.back', handleBack);

    const { children } = props;
    const contextValue = [
        navigationState,
        {
            dispatch,
            showSignIn,
            showMenu,
            showMyAccount,
            showChangePassword,
            showForgotPassword,
            showCreateAccount,
            showAccountCreated
        }
    ];
    return <NavigationContext.Provider value={contextValue}>{children}</NavigationContext.Provider>;
};

NavigationContextProvider.propTypes = {
    initialState: object
};

const withSuspense = NavigationContextProvider => {
    let WithSuspense = props => {
        return (
            <Suspense fallback={<LoadingIndicator />}>
                <NavigationContextProvider {...props} />
            </Suspense>
        );
    };
    WithSuspense.displayName = `withSuspense(${NavigationContextProvider.displayName ||
        NavigationContextProvider.name})`;
    return WithSuspense;
};

export default withSuspense(NavigationContextProvider);

export const useNavigationContext = () => useContext(NavigationContext);
