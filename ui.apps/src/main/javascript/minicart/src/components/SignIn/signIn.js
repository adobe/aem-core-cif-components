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

import { isRequired } from '../../utils/formValidators';
import Button from '../Button';
import Field from '../Field';
import TextInput from '../TextInput';

import classes from './signIn.css';
import { useSignin } from './useSignin';

const SignIn = props => {
    const { showMyAccount, showForgotPassword } = props;
    const { errorMessage, isSignedIn, handleSubmit } = useSignin();

    if (isSignedIn) {
        showMyAccount();
    }

    return (
        <div className={classes.root}>
            <Form onSubmit={handleSubmit} className={classes.form}>
                <Field label="Email" required={true}>
                    <TextInput autoComplete="email" field="email" validate={isRequired} aria-label="email" />
                </Field>
                <Field label="Password" required={true}>
                    <TextInput
                        autoComplete="current-password"
                        field="password"
                        type="password"
                        validate={isRequired}
                        aria-label="password"
                    />
                </Field>
                <div className={classes.signInError}>{errorMessage}</div>
                <div className={classes.signInButton}>
                    <Button priority="high" type="submit" aria-label="submit">
                        {'Sign In'}
                    </Button>
                </div>
                <div className={classes.forgotPasswordButton}>
                    <Button priority="low" type="button" onClick={showForgotPassword}>
                        {'Forgot Password?'}
                    </Button>
                </div>
            </Form>
        </div>
    );
};

SignIn.propTypes = {
    showMyAccount: func.isRequired
};

export default SignIn;
