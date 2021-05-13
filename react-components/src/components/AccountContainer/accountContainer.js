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
import { useTranslation, Trans } from 'react-i18next';

import { useUserContext } from '../../context/UserContext';
import AccountTrigger from './accountTrigger';
import AccountDropdown from './accountDropdown';


import * as dataLayerUtils from '../../utils/dataLayerUtils';

const AccountContainer = props => {
    const [{ currentUser, isSignedIn }, { getUserDetails }] = useUserContext();

    console.log("AuthBar")
    useEffect(() => {
        if (isSignedIn && currentUser.email === '') {
            getUserDetails();
        }
    }, [getUserDetails]);

    useEffect(() => {
        console.log("checking if signed in")
        if (!isSignedIn) {
            dataLayerUtils.pushData({ user: null });
        } else if (isSignedIn && currentUser.email !== '') {
            dataLayerUtils.pushData({ user: currentUser });
        }
    }, [isSignedIn, currentUser]);

    const [t] = useTranslation('account');

    const label = isSignedIn ? (
        <Trans t={t} i18nKey="account:account-icon-text-greeting">
            Hi, {{ name: currentUser.firstname }}
        </Trans>
    ) : (
        t('account:account-icon-text-sign-in', 'Sign In')
    );

    return (
        <>
            <AccountTrigger label={label} />
            <AccountDropdown>{props.children}</AccountDropdown>
        </>
    );
};

export default AccountContainer;
