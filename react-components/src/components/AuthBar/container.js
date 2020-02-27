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
import React, { useState, useCallback, Suspense } from 'react';
import ReactDOM from 'react-dom';
import { useTranslation } from 'react-i18next';

import AuthBar from './authBar';
import AuthModal from '../AuthModal';
import classes from './container.css';
import { useEventListener } from '../../utils/hooks';
import LoadingIndicator from '../LoadingIndicator';

/*
    Views:
        SIGNIN - the signing modal is open
        MENU - the default view
        CREATE_ACCOUNT - the create account modal
        MY_ACCOUNT - the account props modal
        FORGOT_PASSWORD - the forgot password modal
        CHANGE_PASSWORD - the change password modal

*/
// a map of the UI states so we can properly handle the "BACK" button
const ancestors = {
    CREATE_ACCOUNT: 'SIGN_IN',
    FORGOT_PASSWORD: 'SIGN_IN',
    MY_ACCOUNT: 'MENU',
    CHANGE_PASSWORD: 'MY_ACCOUNT',
    SIGN_IN: 'MENU',
    MENU: null
};

// DOM events that are used to talk to the navigation panel
const events = {
    START_ACC_MANAGEMENT: 'aem.accmg.start',
    EXIT_ACC_MANAGEMENT: 'aem.accmg.exit'
};

const startAccMgEvent = new CustomEvent(events.START_ACC_MANAGEMENT);
const exitAccMgEvent = new CustomEvent(events.EXIT_ACC_MANAGEMENT);

const Container = () => {
    const navigationPanel = document.querySelector('aside.navigation__root');
    const container = document.querySelector('#miniaccount');

    const [view, setView] = useState('MENU');

    const hasModal = view !== 'MENU';
    const modalClassName = hasModal ? classes.modal_open : classes.modal;

    const [t] = useTranslation('account');

    const stepTitles = {
        CREATE_ACCOUNT: t('account:create-account', 'Create account'),
        FORGOT_PASSWORD: t('account:password-recovery', 'Password recovery'),
        CHANGE_PASSWORD: t('account:change-password', 'Change Password'),
        MY_ACCOUNT: t('account:my-account', 'My account'),
        SIGN_IN: t('account:sign-in', 'Sign In')
    };

    const handleBack = useCallback(() => {
        if (view === null) {
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
        setView(parent);
    }, [view]);

    const showSignIn = useCallback(() => {
        const view = 'SIGN_IN';
        navigationPanel.dispatchEvent(startAccMgEvent);
        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        setView(view);
    }, [setView]);

    const showMenu = useCallback(() => {
        setView('MENU');
        navigationPanel.dispatchEvent(exitAccMgEvent);
    }, [setView]);

    const showMyAccount = useCallback(() => {
        const view = 'MY_ACCOUNT';
        navigationPanel.dispatchEvent(startAccMgEvent);
        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        setView(view);
    }, [setView]);

    const showChangePassword = useCallback(() => {
        const view = 'CHANGE_PASSWORD';
        navigationPanel.dispatchEvent(startAccMgEvent);
        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        setView(view);
    }, [setView]);

    const showForgotPassword = useCallback(() => {
        const view = 'FORGOT_PASSWORD';
        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        setView(view);
    }, [setView]);

    const showCreateAccount = useCallback(() => {
        const view = 'CREATE_ACCOUNT';
        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: stepTitles[view] } }));
        setView(view);
    }, [setView]);

    useEventListener(document, 'aem.navigation.back', handleBack);

    return ReactDOM.createPortal(
        <>
            <div className="navigation__footer">
                <AuthBar showSignIn={showSignIn} disabled={hasModal} showMyAccount={showMyAccount} />
            </div>
            {view !== null && (
                <div className={modalClassName}>
                    <AuthModal
                        view={view}
                        showMyAccount={showMyAccount}
                        showMenu={showMenu}
                        showForgotPassword={showForgotPassword}
                        showChangePassword={showChangePassword}
                        showCreateAccount={showCreateAccount}
                    />
                </div>
            )}
        </>,
        container
    );
};

const withSuspense = Container => {
    let WithSuspense = props => {
        return (
            <Suspense fallback={<LoadingIndicator />}>
                <Container {...props} />
            </Suspense>
        );
    };
    WithSuspense.displayName = `withSuspense(${Container.displayName || Container.name})`;
    return WithSuspense;
};

export default withSuspense(Container);
