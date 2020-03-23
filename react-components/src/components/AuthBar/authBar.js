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
import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import Button from '../Button';
import classes from './authBar.css';
import { useUserContext } from '../../context/UserContext';
import UserChip from './userChip';
import { func } from 'prop-types';

const AuthBar = props => {
    const { showSignIn, showMyAccount } = props;
    const [{ currentUser, isSignedIn }, { getUserDetails }] = useUserContext();

    useEffect(() => {
        if (isSignedIn && currentUser.email === '') {
            getUserDetails();
        }
    }, [getUserDetails]);
    const [t] = useTranslation('account');

    const disabled = false;

    const content = isSignedIn ? (
        <UserChip currentUser={currentUser} showMyAccount={showMyAccount} />
    ) : (
        <Button disabled={!!disabled} priority="high" onClick={showSignIn}>
            {t('account:sign-in', 'Sign In')}
        </Button>
    );
    return <div className={classes.root}>{content}</div>;
};

AuthBar.propTypes = {
    showSignIn: func.isRequired,
    showMyAccount: func.isRequired
};

export default AuthBar;
