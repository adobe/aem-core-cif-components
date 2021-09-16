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
import React, { useEffect } from 'react';
import { useIntl } from 'react-intl';

import Button from '../Button';
import classes from './authBar.css';
import { useUserContext } from '../../context/UserContext';
import UserChip from './userChip';
import { func } from 'prop-types';

const AuthBar = ({ showMyAccount, showSignIn }) => {
    const [{ currentUser, isSignedIn }, { getUserDetails }] = useUserContext();
    const intl = useIntl();

    useEffect(() => {
        if (isSignedIn && currentUser.email === '') {
            getUserDetails();
        }
    }, [getUserDetails]);

    const content = isSignedIn ? (
        <UserChip currentUser={currentUser} showMyAccount={showMyAccount} />
    ) : (
        <Button disabled={false} priority="high" onClick={showSignIn}>
            {intl.formatMessage({ id: 'account:sign-in', defaultMessage: 'Sign In' })}
        </Button>
    );
    return <div className={classes.root}>{content}</div>;
};

AuthBar.propTypes = {
    showMyAccount: func,
    showSignIn: func
};

export default AuthBar;
