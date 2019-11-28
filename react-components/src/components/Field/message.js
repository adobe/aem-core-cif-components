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
import { node, shape, string } from 'prop-types';

import classes from './message.css';

const Message = props => {
    const { children, fieldState } = props;
    const { asyncError, error } = fieldState;
    const errorMessage = error || asyncError;
    const className = errorMessage ? classes.root_error : classes.root;

    return <p className={className}>{errorMessage || children}</p>;
};

Message.propTypes = {
    children: node,
    classes: shape({
        root: string,
        root_error: string
    }),
    fieldState: shape({
        asyncError: string,
        error: string
    })
};

export default Message;
