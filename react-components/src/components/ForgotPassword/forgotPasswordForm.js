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
import { func, shape, string } from 'prop-types';
import { Form } from 'informed';
import { useTranslation } from 'react-i18next';

import combine from '../../utils/combineValidators';
import { isRequired, validateEmail } from '../../utils/formValidators';
import Field from '../Field';
import TextInput from '../TextInput';
import Button from '../Button';

import defaultClasses from './forgotPasswordForm.css';

const ForgotPasswordForm = props => {
    const { handleFormSubmit, handleCancel } = props;
    const [t] = useTranslation('account');

    const classes = Object.assign({}, defaultClasses, props.classes || {});

    return (
        <Form className={classes.root} onSubmit={handleFormSubmit}>
            <Field label={t('account:email', 'Email address')} required={true}>
                <TextInput
                    autoComplete="email"
                    field="email"
                    validate={combine([isRequired, validateEmail])}
                    validateOnBlur
                    aria-label="email"></TextInput>
            </Field>
            <div className={classes.buttonContainer}>
                <Button disabled={false} type="submit" priority="high" aria-label="submit">
                    {t('account:forgot-password-submit', 'Submit')}
                </Button>
                {handleCancel && (
                    <Button disabled={false} type="button" priority="normal" aria-label="cancel" onClick={handleCancel}>
                        {t('account:forgot-password-cancel', 'Cancel')}
                    </Button>
                )}
            </div>
        </Form>
    );
};

ForgotPasswordForm.propTypes = {
    handleFormSubmit: func.isRequired,
    handleCancel: func,
    classes: shape({
        form: string,
        buttonContainer: string
    })
};

export default ForgotPasswordForm;
