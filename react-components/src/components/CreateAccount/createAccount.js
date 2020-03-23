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
import { shape, string, func } from 'prop-types';
import { useTranslation } from 'react-i18next';

import Field from '../Field';
import TextInput from '../TextInput';
import Checkbox from '../Checkbox';
import Button from '../Button';
import combine from '../../utils/combineValidators';
import {
    validateEmail,
    isRequired,
    validatePassword,
    validateConfirmPassword,
    hasLengthAtLeast
} from '../../utils/formValidators';
import mergeClasses from '../../utils/mergeClasses';
import defaultClasses from './createAccount.css';
import useCreateAccount from './useCreateAccount';
import LoadingIndicator from '../LoadingIndicator';

const CreateAccount = props => {
    const { showMyAccount } = props;
    const [{ createAccountError, isSignedIn, inProgress }, { createAccount }] = useCreateAccount();
    const [t] = useTranslation('account');

    if (inProgress) {
        return <LoadingIndicator message="Creating account" />;
    }

    const handleCreateAccount = formValues => {
        createAccount(formValues);
    };

    const errorMessage = createAccountError ? createAccountError : null;

    if (isSignedIn) {
        showMyAccount();
        return null;
    }
    const classes = mergeClasses(defaultClasses, props.classes);

    return (
        <Form className={classes.root} onSubmit={handleCreateAccount}>
            <p className={classes.lead}>
                {t(
                    'account:create-account-lead',
                    'Check out faster, use multiple addresses, track orders and more by creating an account!'
                )}
            </p>
            <Field label={t('account:firstname', 'First Name')} required={true}>
                <TextInput
                    field="customer.firstname"
                    autoComplete="given-name"
                    validate={isRequired}
                    validateOnBlur
                    aria-label="firstname"
                />
            </Field>
            <Field label={t('account:lastname', 'Last Name')} required={true}>
                <TextInput
                    field="customer.lastname"
                    autoComplete="family-name"
                    validate={isRequired}
                    validateOnBlur
                    aria-label="lastname"
                />
            </Field>
            <Field label={t('account:email', 'E-Mail')} required={true}>
                <TextInput
                    field="customer.email"
                    autoComplete="email"
                    validate={combine([isRequired, validateEmail])}
                    validateOnBlur
                    aria-label="email"
                />
            </Field>
            <Field label={t('account:password', 'Password')} required={true}>
                <TextInput
                    field="password"
                    type="password"
                    autoComplete="new-password"
                    validate={combine([isRequired, [hasLengthAtLeast, 8], validatePassword])}
                    validateOnBlur
                    aria-label="password"
                />
            </Field>
            <Field label={t('account:confirm-password', 'Confirm Password')} required={true}>
                <TextInput
                    field="confirm"
                    type="password"
                    validate={combine([isRequired, validateConfirmPassword])}
                    validateOnBlur
                    aria-label="confirm"
                />
            </Field>
            <div className={classes.subscribe}>
                <Checkbox
                    field="subscribe"
                    label={t('account:subscribe-news', 'Subscribe to news and updates')}
                    aria-label="subscribe"
                />
            </div>
            <div className={classes.error}>{errorMessage}</div>
            <div className={classes.actions}>
                <Button disabled={inProgress} type="submit" priority="high" aria-label="submit">
                    {t('account:create-submit', 'Submit')}
                </Button>
            </div>
        </Form>
    );
};

CreateAccount.propTypes = {
    classes: shape({
        actions: string,
        error: string,
        lead: string,
        root: string,
        subscribe: string
    }),
    showMyAccount: func.isRequired
};

export default CreateAccount;
