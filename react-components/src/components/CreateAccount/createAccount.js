/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import { Form } from 'informed';
import { shape, string, func } from 'prop-types';
import { useIntl } from 'react-intl';

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

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const CreateAccount = props => {
    const { showAccountCreated, handleCancel } = props;
    const [{ createAccountError, inProgress }, { createAccount }] = useCreateAccount({
        showAccountCreated
    });
    const intl = useIntl();

    if (inProgress) {
        return <LoadingIndicator message="Creating account" />;
    }

    const handleCreateAccount = formValues => {
        createAccount(formValues);
    };

    const errorMessage = createAccountError ? createAccountError : null;
    const classes = mergeClasses(defaultClasses, props.classes);

    return (
        <Form className={classes.root} onSubmit={handleCreateAccount}>
            <p className={classes.lead}>
                {intl.formatMessage({
                    id: 'account:create-account-lead',
                    defaultMessage:
                        'Check out faster, use multiple addresses, track orders and more by creating an account!'
                })}
            </p>
            <Field
                label={intl.formatMessage({ id: 'account:firstname', defaultMessage: 'First Name' })}
                required={true}>
                <TextInput
                    field="customer.firstname"
                    autoComplete="given-name"
                    validate={isRequired}
                    validateOnBlur
                    aria-label="firstname"
                />
            </Field>
            <Field label={intl.formatMessage({ id: 'account:lastname', defaultMessage: 'Last Name' })} required={true}>
                <TextInput
                    field="customer.lastname"
                    autoComplete="family-name"
                    validate={isRequired}
                    validateOnBlur
                    aria-label="lastname"
                />
            </Field>
            <Field label={intl.formatMessage({ id: 'account:email', defaultMessage: 'Email address' })} required={true}>
                <TextInput
                    field="customer.email"
                    autoComplete="email"
                    validate={combine([isRequired, validateEmail])}
                    validateOnBlur
                    aria-label="email"
                />
            </Field>
            <Field label={intl.formatMessage({ id: 'account:password', defaultMessage: 'Password' })} required={true}>
                <TextInput
                    field="password"
                    type="password"
                    autoComplete="new-password"
                    validate={combine([isRequired, [hasLengthAtLeast, 8], validatePassword])}
                    validateOnBlur
                    aria-label="password"
                />
            </Field>
            <Field
                label={intl.formatMessage({ id: 'account:confirm-password', defaultMessage: 'Confirm Password' })}
                required={true}>
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
                    label={intl.formatMessage({
                        id: 'account:subscribe-news',
                        defaultMessage: 'Subscribe to news and updates'
                    })}
                    aria-label="subscribe"
                />
            </div>
            <div className={classes.error}>{errorMessage}</div>
            <div className={classes.actions}>
                <Button disabled={inProgress} type="submit" priority="high" aria-label="submit">
                    {intl.formatMessage({ id: 'account:create-submit', defaultMessage: 'Submit' })}
                </Button>
                {handleCancel && (
                    <Button
                        disabled={inProgress}
                        type="button"
                        priority="normal"
                        aria-label="cancel"
                        onClick={handleCancel}>
                        {intl.formatMessage({ id: 'account:create-cancel', defaultMessage: 'Cancel' })}
                    </Button>
                )}
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
    showMyAccount: func.isRequired,
    showAccountCreated: func.isRequired,
    handleCancel: func
};

export default CreateAccount;
