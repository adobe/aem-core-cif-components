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

import AuthBar from './authBar';
import MyAccountPanel from './myAccountPanel';

import classes from './container.css';
import useNavigationState from './useNavigationState';

const Container = () => {
    const [view, api] = useNavigationState();

    const hasModal = view !== 'MENU';
    const modalClassName = hasModal ? classes.modal_open : classes.modal;
    return (
        <>
            <div className="navigation__footer">
                <AuthBar showSignIn={api.showSignIn} showMyAccount={api.showMyAccount} />
            </div>
            {view !== null && (
                <div className={modalClassName}>
                    <MyAccountPanel view={view} api={api} />
                </div>
            )}
        </>
    );
};

export default Container;
