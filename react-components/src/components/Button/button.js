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
import { oneOf, node, shape, string } from 'prop-types';

import defaultClasses from './button.css';

const getRootClassName = priority => `root_${priority}Priority`;

const Button = props => {
    const { children, priority, type, ...restProps } = props;

    let classes = props.classes ? Object.assign({}, defaultClasses, props.classes) : defaultClasses;

    const rootClassName = classes[getRootClassName(priority)];

    return (
        <button className={rootClassName} type={type} {...restProps}>
            <span className={classes.content}>{children}</span>
        </button>
    );
};

Button.defaultProps = {
    priority: 'normal',
    type: 'button'
};

Button.propTypes = {
    priority: oneOf(['high', 'normal', 'low']).isRequired,
    type: oneOf(['button', 'reset', 'submit']).isRequired,
    children: node,
    classes: shape({
        content: string
    })
};

export default Button;
