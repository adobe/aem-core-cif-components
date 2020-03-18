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
import { string, func, shape } from 'prop-types';
import { useTranslation, Trans } from 'react-i18next';

import Button from '../Button';

import defaultClasses from './formSubmissionSuccessful.css';
import mergeClasses from '../../utils/mergeClasses';

const FormSubmissionSuccessful = props => {
    const { email, onContinue } = props;
    const [t] = useTranslation('account');

    const classes = mergeClasses(defaultClasses, props.classes || {});

    return (
        <div className={classes.root}>
            <p className={classes.text}>
                <Trans i18nKey="account:forget-password-confirmation">
                    If there is an account associated with {{ email }}, you will receive an email with a link to change
                    your password.
                </Trans>
            </p>
            <div className={classes.buttonContainer}>
                <Button aria-label="continue-shopping" onClick={onContinue}>
                    {t('account:continue-shopping', 'Continue Shopping')}
                </Button>
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
