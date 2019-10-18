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
import { LogOut as SignOutIcon } from 'react-feather';

import AccountLink from './accountLink';
import LoadingIndicator from '../LoadingIndicator';

import classes from './myAccount.css';
import { useUserContext } from '../../context/UserContext';
import { func } from 'prop-types';

const SIGN_OUT = 'Sign Out';

const MyAccount = props => {
    const { showMenu } = props;
    const [{ currentUser, isSignedIn, inProgress }, { signOut }] = useUserContext();

    if (inProgress) {
        return (
            <div className={classes.modal_active}>
                <LoadingIndicator>{'Signing In'}</LoadingIndicator>
            </div>
        );
    }

    if (!isSignedIn) {
        showMenu();
    }

    return (
        <div className={classes.root}>
            <div className={classes.user}>
                <h2 className={classes.title}>{`${currentUser.firstname} ${currentUser.lastname}`}</h2>
                <span className={classes.subtitle}>{currentUser.email}</span>
            </div>
            <div className={classes.actions}>
                <AccountLink onClick={signOut}>
                    <SignOutIcon size={18} />
                    {SIGN_OUT}
                </AccountLink>
            </div>
        </div>
    );
};

MyAccount.propTypes = {
    showMenu: func.isRequired
};

export default MyAccount;
