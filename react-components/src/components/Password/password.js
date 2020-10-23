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
import React from 'react';
import { string, bool, shape, func } from 'prop-types';
import { Eye, EyeOff } from 'react-feather';

import usePassword from './usePassword';
import mergeClasses from '../../utils/mergeClasses';

import Button from '../Button';
import Field from '../Field';
import TextInput from '../TextInput';
import { isRequired } from '../../utils/formValidators';

import defaultClasses from './password.css';

const Password = props => {
    const {
        classes: propClasses,
        label,
        fieldName,
        isToggleButtonHidden,
        autoComplete,
        validate,
        ...otherProps
    } = props;
    const talonProps = usePassword();
    const { visible, togglePasswordVisibility } = talonProps;
    const classes = mergeClasses(defaultClasses, propClasses);

    const passwordButton = !isToggleButtonHidden ? (
        <Button className={classes.passwordButton} onClick={togglePasswordVisibility} type="button">
            {visible ? <Eye /> : <EyeOff />}
        </Button>
    ) : null;

    const fieldType = visible ? 'text' : 'password';

    return (
        <Field label={label} classes={{ root: classes.root }}>
            <TextInput
                after={passwordButton}
                autoComplete={autoComplete}
                field={fieldName}
                type={fieldType}
                validate={validate}
                {...otherProps}
            />
        </Field>
    );
};

Password.propTypes = {
    autoComplete: string,
    classes: shape({
        root: string
    }),
    label: string,
    fieldName: string,
    isToggleButtonHidden: bool,
    validate: func
};

Password.defaultProps = {
    isToggleButtonHidden: true,
    validate: isRequired
};

export default Password;
