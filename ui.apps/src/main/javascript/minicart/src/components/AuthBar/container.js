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
import React, { useState, useCallback } from 'react';
import ReactDOM from 'react-dom';
import AuthBar from './authBar';
import AuthModal from '../AuthModal';
import classes from './container.css';
import { useEventListener } from '../../utils/hooks';

/*
    Views:
        SIGNIN - the signing modal is open
        MENU - the default view
        CREATE_ACCOUNT - the create account modal
        MY_ACCOUNT - the account props modal
        FORGOT_PASSWORD - the forgot password modal

*/
// a map of the UI states so we can properly handle the "BACK" button
const ancestors = {
    CREATE_ACCOUNT: 'SIGN_IN',
    FORGOT_PASSWORD: 'SIGN_IN',
    MY_ACCOUNT: 'MENU',
    SIGN_IN: 'MENU',
    MENU: null
};

// DOM events that are used to talk to the navigation panel
const events = {
    START_ACC_MANAGEMENT: 'aem.accmg.start'
};

const Container = props => {
    const navigationPanel = document.querySelector('aside.navigation__root');
    const container = document.querySelector('.account_management_root');
    useEventListener(document, 'aem.navigation.back', handleBack);

    const [view, setView] = useState('MENU');

    const hasModal = view !== 'MENU';
    const modalClassName = hasModal ? classes.modal_open : classes.modal;

    const handleBack = useCallback(() => {
        if (view === null) {
            return;
        }
        const parent = ancestors[view];
        console.log(`Switching to view ${parent}`);
        setView(parent);
    }, [view]);

    const showSignIn = useCallback(() => {
        console.log(`Showing SIGN_IN view`);
        const ev = new CustomEvent(events.START_ACC_MANAGEMENT);
        navigationPanel.dispatchEvent(ev);
        setView('SIGN_IN');
    }, [setView]);

    return ReactDOM.createPortal(
        <>
            <div className="navigation__footer">
                <AuthBar showSignIn={showSignIn} disabled={hasModal} />
            </div>
            {view !== null && (
                <div className={modalClassName}>
                    <AuthModal view={view} />
                </div>
            )}
        </>,
        container
    );
};

export default Container;
