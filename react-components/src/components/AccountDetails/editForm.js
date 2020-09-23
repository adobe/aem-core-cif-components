/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import React, { Fragment } from 'react';
import { bool, func } from 'prop-types';

import Field from '../Field';
import LinkButton from '../LinkButton';
import TextInput from '../TextInput';
import { isRequired } from '../../utils/formValidators';
import { useTranslation } from 'react-i18next';

import classes from './editForm.css';

const EditForm = props => {
    const { shouldShowNewPassword, handleChangePassword } = props;
    const [t] = useTranslation('account');
    const maybeShowChangePasswordButton = !shouldShowNewPassword && (
        <div className={classes.changePasswordButtonContainer}>
            <LinkButton type="button" classes={classes.changePasswordButton} onClick={handleChangePassword}>
                {t('account:change-password', 'Change Password')}
            </LinkButton>
        </div>
    );

    return (
        <Fragment>
            <div className={classes.root}>
                <div className={classes.firstname}>
                    <Field id="firstname" label={t('account:firstname', 'First Name')}>
                        <TextInput field="firstname" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.lastname}>
                    <Field id="lastname" label={t('account:lastname', 'Last Name')}>
                        <TextInput field="lastname" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.email}>
                    <Field id="email" label={t('account:email', 'Email address')}>
                        <TextInput field="email" validate={isRequired} />
                    </Field>
                </div>
                {maybeShowChangePasswordButton}
            </div>
        </Fragment>
    );
};

export default EditForm;

EditForm.propTypes = {
    shouldShowNewPassword: bool,
    handleChangePassword: func
};
