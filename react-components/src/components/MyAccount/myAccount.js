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
import React from 'react';
import { useTranslation } from 'react-i18next';
import { func } from 'prop-types';

import LoadingIndicator from '../LoadingIndicator';

import classes from './myAccount.css';
import { useUserContext } from '../../context/UserContext';

import { SignOutLink, AccountInfoLink, AddressBookLink, ChangePasswordLink } from './AccountLinks';

const MyAccount = props => {
    const { showMenu, showChangePassword, showAccountInformation } = props;
    const [{ currentUser, inProgress }] = useUserContext();
    const [t] = useTranslation('account');

    if (inProgress) {
        return (
            <div className={classes.modal_active}>
                <LoadingIndicator>{t('account:signing-in', 'Signing In')}</LoadingIndicator>
            </div>
        );
    }

    const accountLinks = props.children || (
        <>
            <ChangePasswordLink showChangePassword={showChangePassword} />
            <AddressBookLink />
            <AccountInfoLink showAccountInformation={showAccountInformation} />
            <SignOutLink showMenu={showMenu} />
        </>
    );

    return (
        <div className={classes.root}>
            <div className={classes.user}>
                <h2 className={classes.title}>{`${currentUser.firstname} ${currentUser.lastname}`}</h2>
                <span className={classes.subtitle}>{currentUser.email}</span>
            </div>
            <div className={classes.actions}>{accountLinks}</div>
        </div>
    );
};

MyAccount.propTypes = {
    showMenu: func,
    showAccountInformation: func.isRequired,
    showChangePassword: func.isRequired
};

export default MyAccount;
