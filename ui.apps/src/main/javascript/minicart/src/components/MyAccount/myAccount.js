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
import { Archive as HistoryIcon, LogOut as SignOutIcon } from 'react-feather';
import { func, shape, string } from 'prop-types';

import AccountLink from './accountLink';
import classes from './myAccount.css';
import { useUserContext } from '../../context/UserContext';

const PURCHASE_HISTORY = 'Purchase History';
const SIGN_OUT = 'Sign Out';

const MyAccount = props => {
    const [{ currentUser }] = useUserContext();

    return (
        <div className={classes.root}>
            <div className={classes.user}>
                <h2 className={classes.title}>{`${currentUser.firstname} ${currentUser.lastname}`}</h2>
                <span className={classes.subtitle}>{currentUser.email}</span>
            </div>
            <div className={classes.actions}>
                <AccountLink>
                    <HistoryIcon size={18} />
                    {PURCHASE_HISTORY}
                </AccountLink>
                <AccountLink onClick={() => {}}>
                    <SignOutIcon size={18} />
                    {SIGN_OUT}
                </AccountLink>
            </div>
        </div>
    );
};

export default MyAccount;
