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
import React, { Fragment } from 'react';
import { node, shape, string } from 'prop-types';
import { BasicText, asField } from 'informed';

import { FieldIcons, Message } from '../Field';
import classes from './textInput.css';

const TextInput = props => {
    const { after, before, fieldState, message, ...rest } = props;

    return (
        <Fragment>
            <FieldIcons after={after} before={before}>
                <BasicText {...rest} fieldState={fieldState} className={classes.input} />
            </FieldIcons>
            <Message fieldState={fieldState}>{message}</Message>
        </Fragment>
    );
};

TextInput.propTypes = {
    after: node,
    before: node,
    classes: shape({
        input: string
    }),
    fieldState: shape({
        value: string
    }),
    message: node
};

export default asField(TextInput);
