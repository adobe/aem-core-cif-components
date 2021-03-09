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
import { bool, node, shape, string } from 'prop-types';

import classes from './field.css';

const Field = props => {
    const { required, children, label, htmlFor } = props;

    return (
        <div className={classes.root}>
            <label htmlFor={htmlFor}>
                <span className={classes.label}>
                    {required && <span className={classes.requiredSymbol} />}
                    {label}
                </span>
            </label>
            {children}
        </div>
    );
};

Field.propTypes = {
    children: node,
    classes: shape({
        label: string,
        root: string
    }),
    label: node,
    required: bool,
    htmlFor: string
};

export default Field;
