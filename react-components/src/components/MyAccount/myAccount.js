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
import { Book as BookIcon, Lock as PasswordIcon, LogOut as SignOutIcon, Info as InfoIcon } from 'react-feather';
import { useTranslation } from 'react-i18next';
import { func } from 'prop-types';

import AccountLink from './accountLink';
import LoadingIndicator from '../LoadingIndicator';

import classes from './myAccount.css';
import { useUserContext } from '../../context/UserContext';
import { useCartState } from '../Minicart/cartContext';
import { useConfigContext } from '../../context/ConfigContext';

const MyAccount = props => {
    const { showMenu, showChangePassword, showAccountInformation } = props;
    const [{ currentUser, inProgress }, { signOut }] = useUserContext();
    const { pagePaths } = useConfigContext();
    const [, dispatch] = useCartState();
    const [t] = useTranslation('account');

    if (inProgress) {
        return (
            <div className={classes.modal_active}>
                <LoadingIndicator>{t('account:signing-in', 'Signing In')}</LoadingIndicator>
            </div>
        );
    }

    const handleSignOut = async () => {
        dispatch({ type: 'reset' });
        await signOut();
        if (showMenu) {
            showMenu();
        }
    };

    return (
        <div className={classes.root}>
            <div className={classes.user}>
                <h2 className={classes.title}>{`${currentUser.firstname} ${currentUser.lastname}`}</h2>
                <span className={classes.subtitle}>{currentUser.email}</span>
            </div>
            <div className={classes.actions}>
                <AccountLink onClick={showChangePassword}>
                    <PasswordIcon size={18} />
                    {t('account:change-password', 'Change Password')}
                </AccountLink>
                <AccountLink
                    onClick={() => {
                        window.location.href = pagePaths.addressBook;
                    }}>
                    <BookIcon size={18} />
                    {t('account:address-book', 'Address Book')}
                </AccountLink>
                <AccountLink onClick={showAccountInformation}>
                    <InfoIcon size={18} />
                    {t('account:account-information', 'Account Information')}
                </AccountLink>
                <AccountLink onClick={handleSignOut}>
                    <SignOutIcon size={18} />
                    {t('account:sign-out', 'Sign Out')}
                </AccountLink>
            </div>
        </div>
    );
};

MyAccount.propTypes = {
    showMenu: func,
    showAccountInformation: func.isRequired,
    showChangePassword: func.isRequired
};

export default MyAccount;
