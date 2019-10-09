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

import Button from '../Button';

import defaultClasses from './formSubmissionSuccessful.css';
import mergeClasses from '../../utils/mergeClasses';
import { string, func, shape } from 'prop-types';

const FormSubmissionSuccessful = props => {
    const { email, onContinue } = props;

    const classes = mergeClasses(defaultClasses, props.classes || {});

    const textMessage = `If there is an account associated with ${email}, you will receive an email with a link to change your password.`;
    const CONTINUE_SHOPPING = 'Continue Shopping';

    return (
        <div className={classes.root}>
            <p className={classes.text}>{textMessage}</p>
            <div className={classes.buttonContainer}>
                <Button onClick={onContinue}>{CONTINUE_SHOPPING}</Button>
            </div>
        </div>
    );
};

FormSubmissionSuccessful.propTypes = {
    email: string.isRequired,
    onContinue: func.isRequired,
    classes: shape({
        buttonContainer: string,
        root: string,
        text: string
    })
};

export default FormSubmissionSuccessful;
