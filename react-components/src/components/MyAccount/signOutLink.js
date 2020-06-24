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
import { useTranslation } from 'react-i18next';

import AccountLink from './accountLink';

import { useUserContext } from '../../context/UserContext';
import { useCartState } from '../Minicart/cartContext';

const SignOutLink = () => {
    const [, { signOut }] = useUserContext();
    const [, dispatch] = useCartState();

    const handleSignOut = () => {
        dispatch({ type: 'reset' });
        signOut();
    };

    const [t] = useTranslation('account');

    return (
        <AccountLink onClick={handleSignOut}>
            <SignOutIcon size={18} />
            {t('account:sign-out', 'Sign Out')}
        </AccountLink>
    );
};

export default SignOutLink;
