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
import { func } from 'prop-types';
import { useTranslation } from 'react-i18next';

import { isRequired } from '../../utils/formValidators';
import Button from '../Button';
import Field from '../Field';
import TextInput from '../TextInput';

import classes from './signIn.css';
import { useSignin } from './useSignin';
import LoadingIndicator from '../LoadingIndicator';

const SignIn = props => {
    const { showMyAccount, showForgotPassword, showCreateAccount } = props;
    const { errorMessage, handleSubmit, inProgress } = useSignin({ showMyAccount });
    const [t] = useTranslation('account');

    if (inProgress) {
        return <LoadingIndicator>{t('account:signing-in', 'Signing In')}</LoadingIndicator>;
    }

    return (
        <div className={classes.root}>
            <Form onSubmit={handleSubmit} className={classes.form}>
                <div className={classes.formTitle}>{t('account:sign-in-form-title', 'Sign-in to Your Account')}</div>
                <Field label={t('account:email', 'Email address')} required={true}>
                    <TextInput autoComplete="email" field="email" validate={isRequired} aria-label="email" />
                </Field>
                <Field label={t('account:password', 'Password')} required={true}>
                    <TextInput
                        autoComplete="current-password"
                        field="password"
                        type="password"
                        validate={isRequired}
                        aria-label="password"
                    />
                </Field>
                <div className={classes.forgotPasswordButton}>
                    <Button priority="low" type="button" onClick={showForgotPassword}>
                        {t('account:forgot-password', 'Forgot Password?')}
                    </Button>
                </div>
                <div className={classes.signInError}>{errorMessage}</div>
                <div className={classes.signInButton}>
                    <Button priority="high" type="submit" aria-label="submit">
                        {t('account:sign-in', 'Sign In')}
                    </Button>
                </div>
                <div className={classes.createAccountButton}>
                    <Button priority="normal" type="button" onClick={showCreateAccount}>
                        {t('account:create-an-account', 'Create an Account')}
                    </Button>
                </div>
            </Form>
        </div>
    );
};

SignIn.propTypes = {
    showMyAccount: func.isRequired,
    showForgotPassword: func.isRequired,
    showCreateAccount: func.isRequired
};

export default SignIn;
