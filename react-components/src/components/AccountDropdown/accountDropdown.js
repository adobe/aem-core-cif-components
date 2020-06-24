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
import ReactDOM from 'react-dom';
import { useTranslation } from 'react-i18next';

import { useUserContext } from '../../context/UserContext';
import { useSignin } from '../SignIn/useSignin';
import LoadingIndicator from '../LoadingIndicator';
import SignOutLink from '../MyAccount/signOutLink';
import SignInForm from '../SignIn/signInForm';

import classes from './accountDropdown.css';

const AccountDropdown = () => {
    const container = document.querySelector('#account');
    const [{ isAccountDropdownOpen }] = useUserContext();
    const { errorMessage, isSignedIn, handleSubmit, inProgress } = useSignin();

    const [t] = useTranslation('account');

    const dropdownClassName = isAccountDropdownOpen ? classes.dropdown_open : classes.dropdown;
    let dropdownContent = isSignedIn ? (
        <SignOutLink />
    ) : (
        <SignInForm errorMessage={errorMessage} handleSubmit={handleSubmit} />
    );

    if (inProgress) {
        dropdownContent = <LoadingIndicator>{t('account:signing-in', 'Signing In')}</LoadingIndicator>;
    }

    return ReactDOM.createPortal(<div className={dropdownClassName}>{dropdownContent}</div>, container);
};

export default AccountDropdown;
