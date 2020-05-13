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
import { useTranslation, Trans } from 'react-i18next';
import { string } from 'prop-types';

import Trigger from '../Trigger';
import { useNavigationContext } from '../../context/NavigationContext';
import classes from './createAccountSuccess.css';

const CreateAccountSuccess = props => {
    const [t] = useTranslation('account');
    const { email } = props;
    const [, { showSignIn }] = useNavigationContext();

    return (
        <div className={classes.root}>
            <div className={classes.body}>
                <h2 className={classes.header}>
                    {t('account:account-created-title', 'Your account was succesfully created')}
                </h2>
                <div className={classes.textBlock}>
                    {/* prettier-ignore */}
                    <Trans i18nKey="account:email-confirmation-info">
                        You will receive a link at {{ email }}. Access that link to confirm your email address.
                    </Trans>
                </div>
                <Trigger action={showSignIn}>
                    <span className={classes.signin}>{t('account:sign-in', 'Sign In')}</span>
                </Trigger>
            </div>
        </div>
    );
};
CreateAccountSuccess.propTypes = {
    email: string.isRequired
};
export default CreateAccountSuccess;
