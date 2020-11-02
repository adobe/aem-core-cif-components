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
import { oneOf, shape, string } from 'prop-types';

import mergeClasses from '../../utils/mergeClasses';
import defaultClasses from './linkButton.css';
import Button from '../Button';

/**
 * A component for link buttons.
 */
const LinkButton = props => {
    const { children, classes: propClasses, type, ...restProps } = props;
    const classes = mergeClasses(defaultClasses, propClasses);

    return (
        <Button priority={'normal'} classes={{ root_normalPriority: classes.root }} type={type} {...restProps}>
            {children}
        </Button>
    );
};

LinkButton.propTypes = {
    classes: shape({
        root: string
    }),
    type: oneOf(['button', 'reset', 'submit']).isRequired
};

LinkButton.defaultProps = {
    type: 'button'
};

export default LinkButton;
