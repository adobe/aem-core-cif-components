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

import classes from './changePassword.css';
import { useUserContext } from '../../context/UserContext';
import { func } from 'prop-types';
import { Form } from 'informed';

import Field from '../Field';
import TextInput from '../TextInput';
import Button from '../Button';
import combine from '../../utils/combineValidators';
import {
    isRequired,
    validatePassword,
    validateConfirmPassword,
    hasLengthAtLeast
} from '../../utils/formValidators';

const ChangePassword = props => {
    const { showMyAccount } = props;
    const [{ currentUser }] = useUserContext();

    const errorMessage = '';

    return (
        <div className={classes.root}>
            <Form onSubmit={() => {}}>
                <Field label="Current Password" required={true}>
                    <TextInput
                        field="old-password"
                        autoComplete="old-password"
                        validate={combine([isRequired])}
                        validateOnBlur
                        aria-label="old-password"
                    />
                </Field>
                <Field label="New Password" required={true}>
                    <TextInput
                        field="password"
                        type="password"
                        autoComplete="password"
                        validate={combine([isRequired, [hasLengthAtLeast, 8], validatePassword])}
                        validateOnBlur
                        aria-label="password"
                    />
                </Field>
                <Field label="Confirm New Password" required={true}>
                    <TextInput
                        field="confirm"
                        type="password"
                        validate={combine([isRequired, validateConfirmPassword])}
                        validateOnBlur
                        aria-label="confirm"
                    />
                </Field>
                <div className={classes.error}>{errorMessage}</div>
                <div className={classes.actions}>
                    <Button type="submit" priority="high" aria-label="submit">{'Change Password'}</Button>
                </div>
            </Form>
        </div>
    );
};

ChangePassword.propTypes = {
    showMyAccount: func.isRequired,
};

export default ChangePassword;
