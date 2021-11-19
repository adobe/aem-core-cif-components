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
import { func, shape, string } from 'prop-types';
import { Form } from 'informed';
import { useIntl } from 'react-intl';

import combine from '../../utils/combineValidators';
import { isRequired, validateEmail } from '../../utils/formValidators';
import Field from '../Field';
import TextInput from '../TextInput';
import Button from '../Button';

import defaultClasses from './forgotPasswordForm.css';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const ForgotPasswordForm = props => {
    const { handleFormSubmit, handleCancel } = props;
    const intl = useIntl();

    const classes = Object.assign({}, defaultClasses, props.classes || {});

    return (
        <Form className={classes.root} onSubmit={handleFormSubmit}>
            <Field label={intl.formatMessage({ id: 'account:email', defaultMessage: 'Email address' })} required={true}>
                <TextInput
                    autoComplete="email"
                    field="email"
                    validate={combine([isRequired, validateEmail])}
                    validateOnBlur
                    aria-label="email"></TextInput>
            </Field>
            <div className={classes.buttonContainer}>
                <Button disabled={false} type="submit" priority="high" aria-label="submit">
                    {intl.formatMessage({ id: 'account:forgot-password-submit', defaultMessage: 'Submit' })}
                </Button>
                {handleCancel && (
                    <Button disabled={false} type="button" priority="normal" aria-label="cancel" onClick={handleCancel}>
                        {intl.formatMessage({ id: 'account:forgot-password-cancel', defaultMessage: 'Cancel' })}
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
