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
import ReactDOM from 'react-dom';

import AuthBar from './authBar';
import AuthModal from '../AuthModal';
import classes from './container.css';

import { useNavigationContext } from '../../context/NavigationContext';



const Container = () => {
    const container = document.querySelector('#miniaccount');
    const [{ view }, {
        showSignIn,
        showMyAccount,
        showMenu,
        showForgotPassword,
        showChangePassword,
        showCreateAccount
    }] = useNavigationContext();

    const hasModal = view !== 'MENU';
    const modalClassName = hasModal ? classes.modal_open : classes.modal;


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

export default Container;
