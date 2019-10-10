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
import classes from './authModal.css';

import SignIn from '../SignIn';
import MyAccount from '../MyAccount';
import { string, func } from 'prop-types';

const AuthModal = props => {
    const { view, showMyAccount, showMenu } = props;

    let child;

    switch (view) {
        case 'SIGN_IN':
            child = <SignIn showMyAccount={showMyAccount} />;
            break;
        case 'MY_ACCOUNT':
            child = <MyAccount showMenu={showMenu} />;
            break;
    }

    return <div className={classes.root}>{child}</div>;
};

AuthModal.propTypes = {
    view: string.isRequired,
    showMyAccount: func.isRequired,
    showMenu: func.isRequired
};

export default AuthModal;
