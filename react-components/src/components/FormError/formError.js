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

import React, { useRef, useEffect } from 'react';
import { arrayOf, bool, instanceOf, shape, string } from 'prop-types';

import defaultClasses from './formError.css';
import mergeClasses from '../../utils/mergeClasses';
import { deriveErrorMessage } from '../../utils/deriveErrorMessage';

const FormError = props => {
    const { classes: propClasses, errors, scrollOnError } = props;

    const errorMessage = deriveErrorMessage(errors);

    const errorRef = useRef(null);
    useEffect(() => {
        if (scrollOnError && errorMessage) {
            errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }, [errorRef, scrollOnError, errorMessage]);

    const classes = mergeClasses(defaultClasses, propClasses);

    return errorMessage ? (
        <div className={classes.root} ref={errorRef}>
            <span className={classes.errorMessage}>{errorMessage}</span>
        </div>
    ) : null;
};

export default FormError;

FormError.propTypes = {
    classes: shape({
        root: string,
        errorMessage: string
    }),
    errors: arrayOf(instanceOf(Error)),
    scrollOnError: bool
};

FormError.defaultProps = {
    scrollOnError: true
};
