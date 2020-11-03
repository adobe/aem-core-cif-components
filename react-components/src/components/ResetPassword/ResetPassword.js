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
import { Form } from 'informed';
import { useTranslation } from 'react-i18next';

import { useQueryParams } from '../../utils/hooks';
import Field from '../Field';
import TextInput from '../TextInput';
import Button from '../Button';
import combine from '../../utils/combineValidators';
import {
    isRequired,
    validateEmail,
    validatePassword,
    validateConfirmPassword,
    hasLengthAtLeast
} from '../../utils/formValidators';
import useResetPassword from './useResetPassword';
import LoadingIndicator from '../LoadingIndicator';

import classes from './ResetPassword.css';

const ResetPassword = () => {
    const [t] = useTranslation('account', 'common');
    const queryParams = useQueryParams();
    const token = queryParams.get('token');
    const [status, { handleFormSubmit }] = useResetPassword();

    const onSubmit = formValues => {
        const { email, password } = formValues;
        handleFormSubmit({ email, password, token });
    };

    if (!token) {
        return (
            <div className={classes.error}>
                {t('account:reset-password-missing-token', 'Missing or invalid token.')}
            </div>
        );
    }

    if (status === 'loading') {
        return <LoadingIndicator>{t('common:loading', 'Loading')}</LoadingIndicator>;
    }

    if (status === 'error') {
        return <div className={classes.error}>{t('account:reset-password-error', 'Could not reset password.')}</div>;
    }

    if (status === 'done') {
        return (
            <div className={classes.root}>
                <p className={classes.lead}>
                    {t(
                        'account:reset-password-success',
                        'Your password was changed. Please log in with your new password.'
                    )}
                </p>
            </div>
        );
    }

    return (
        <Form className={classes.root} onSubmit={onSubmit}>
            <p className={classes.lead}>
                {t('account:reset-password-lead', 'Please choose a new password to complete the password reset.')}
            </p>
            <div className={classes.fields}>
                <Field label={t('account:email', 'Email address')} required={true}>
                    <TextInput
                        field="email"
                        autoComplete="email"
                        validate={combine([isRequired, validateEmail])}
                        validateOnBlur
                        aria-label="email"
                    />
                </Field>
                <Field label={t('account:new-password', 'New Password')} required={true}>
                    <TextInput
                        field="password"
                        type="password"
                        autoComplete="new-password"
                        validate={combine([isRequired, [hasLengthAtLeast, 8], validatePassword])}
                        validateOnBlur
                        aria-label="password"
                    />
                </Field>
                <Field label={t('account:new-password-confirm', 'Confirm new Password')} required={true}>
                    <TextInput
                        field="confirm"
                        type="password"
                        validate={combine([isRequired, validateConfirmPassword])}
                        validateOnBlur
                        aria-label="confirm"
                    />
                </Field>
            </div>
            <div className={classes.submit}>
                <Button type="submit" priority="high" aria-label="submit">
                    {t('account:reset-password', 'Reset Password')}
                </Button>
            </div>
        </Form>
    );
};

export default ResetPassword;
