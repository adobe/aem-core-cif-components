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

import classes from './label.css';

const Label = props => {
    const { children, plain, ...restProps } = props;
    const elementType = plain ? 'span' : 'label';
    const labelProps = {
        ...restProps,
        className: classes.root
    };

    return React.createElement(elementType, labelProps, children);
};

Label.propTypes = {
    children: node,
    classes: shape({
        root: string
    }),
    plain: bool
};

export default Label;
