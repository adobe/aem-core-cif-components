/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import AccountLink from '../accountLink';
import { useTranslation } from 'react-i18next';
import { LogOut as SignOutIcon } from 'react-feather';
import { useUserContext } from '../../../context/UserContext';
import { useCartState } from '../../../components/Minicart/cartContext';
import { useConfigContext } from '../../../context/ConfigContext';

import { func } from 'prop-types';

const SignOutLink = props => {
    const { showMenu } = props;
    const [t] = useTranslation('account');
    const [, dispatch] = useCartState();
    const { pagePaths } = useConfigContext();
    const [, { signOut }] = useUserContext();

    const handleSignOut = async () => {
        dispatch({ type: 'reset' });
        await signOut();
        if (showMenu) {
            showMenu();
        }
        redirectBacktoStorefront();
    };

    const redirectBacktoStorefront = () => {
        if (pagePaths && pagePaths.baseUrl) {
            window.location.href = pagePaths.baseUrl;
        }
    };

    return (
        <AccountLink onClick={handleSignOut}>
            <SignOutIcon size={18} />
            {t('account:sign-out', 'Sign Out')}
        </AccountLink>
    );
};

SignOutLink.propTypes = {
    showMenu: func
};

export default SignOutLink;
