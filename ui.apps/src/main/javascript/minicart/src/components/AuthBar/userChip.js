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
import { ChevronRight as ChevronRightIcon, User as UserIcon } from 'react-feather';
import classes from './userChip.css';
import Icon from '../Icon';
import { shape, string, func } from 'prop-types';

const UserChip = props => {
    const { currentUser, showMyAccount } = props;
    const { email, firstname, lastname } = currentUser;

    const display = `${firstname} ${lastname}`;

    return (
        <button className={classes.root} onClick={showMyAccount}>
            <span className={classes.content}>
                <span className={classes.avatar}>
                    <Icon src={UserIcon} />
                </span>
                <span className={classes.user}>
                    <span className={classes.fullName}>{display}</span>
                    <span className={classes.email}>{email}</span>
                </span>
                <span className={classes.icon}>
                    <Icon src={ChevronRightIcon} />
                </span>
            </span>
        </button>
    );
};

UserChip.propTypes = {
    currentUser: shape({
        firstname: string,
        lastname: string,
        email: string
    }).isRequired,
    showMyAccount: func.isRequired
};

export default UserChip;
