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
import ForgotPasswordForm from './forgotPasswordForm';

import classes from './forgotPassword.css';
import FormSubmissionSubccessful from './formSubmissionSuccessful';
import useForgotPassword from './useForgotPassword';

const INSTRUCTIONS = 'Enter your email below to receive a password reset link.';

const ForgotPassword = props => {
    const { onClose } = props;

    const [{ submitting, email }, { handleContinue, handleFormSubmit }] = useForgotPassword({ onClose });

    console.log(`Form submitting ${submitting}`);

    const content = submitting ? (
        <FormSubmissionSubccessful email={email} onContinue={handleContinue}></FormSubmissionSubccessful>
    ) : (
        <>
            <p className={classes.instructions}>{INSTRUCTIONS}</p>
            <ForgotPasswordForm handleFormSubmit={handleFormSubmit} />
        </>
    );

    return <div className={classes.root}>{content}</div>;
};

export default ForgotPassword;
