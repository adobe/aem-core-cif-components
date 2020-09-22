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
import { func } from 'prop-types';
import { useTranslation } from 'react-i18next';

import ForgotPasswordForm from './forgotPasswordForm';
import FormSubmissionSuccessful from './formSubmissionSuccessful';
import useForgotPassword from './useForgotPassword';
import LoadingIndicator from '../LoadingIndicator';

import classes from './forgotPassword.css';

const ForgotPassword = props => {
    const { onClose, onCancel } = props;

    const [{ loading, submitted, email }, { handleFormSubmit }] = useForgotPassword();
    const [t] = useTranslation('account', 'common');

    let content;

    if (loading) {
        content = <LoadingIndicator>{t('common:loading', 'Loading')}</LoadingIndicator>;
    } else if (submitted) {
        content = <FormSubmissionSuccessful email={email} onContinue={onClose} />;
    } else {
        content = (
            <>
                <p className={classes.instructions}>
                    {t(
                        'account:reset-password-instructions',
                        'Enter your email below to receive a password reset link.'
                    )}
                </p>
                <ForgotPasswordForm handleFormSubmit={handleFormSubmit} handleCancel={onCancel} />
            </>
        );
    }

    return <div className={classes.root}>{content}</div>;
};

ForgotPassword.propTypes = {
    onClose: func.isRequired,
    onCancel: func.isRequired
};

export default ForgotPassword;
