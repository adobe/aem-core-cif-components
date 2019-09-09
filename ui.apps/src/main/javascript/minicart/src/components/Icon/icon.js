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
import { func, number, object, oneOfType, shape, string } from 'prop-types';

import classes from './icon.css';

/**
 * The Icon component allows us to wrap each icon with some default styling.
 */

const Icon = props => {
    const { attrs: { width, ...restAttrs } = {}, size, src: IconComponent } = props;

    // Permit both prop styles:
    // <Icon src={Foo} attrs={{ width: 18 }} />
    // <Icon src={Foo} size={18} />
    return (
        <span className={classes.root}>
            <IconComponent size={size || width} {...restAttrs} />
        </span>
    );
};

Icon.propTypes = {
    classes: shape({
        root: string
    }),
    size: number,
    attrs: object,
    src: oneOfType([func, shape({ render: func.isRequired })]).isRequired
};

export default Icon;
