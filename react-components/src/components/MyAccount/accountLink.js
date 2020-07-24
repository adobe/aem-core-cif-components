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
import React, { useCallback } from 'react';
import { arrayOf, func, node, shape, string } from 'prop-types';

import Button from '../Button';

import defaultClasses from './accountLink.css';

const AccountLink = props => {
    const { children, onClick } = props;
    const [icon, text] = children;
    const classes = Object.assign({}, defaultClasses, props.classes);

    const handleClick = useCallback(() => {
        if (typeof onClick === 'function') {
            onClick();
        }
    }, [onClick]);

    return (
        <Button classes={classes} onClick={handleClick}>
            <span className={classes.icon}>{icon}</span>
            <span className={classes.text}>{text}</span>
        </Button>
    );
};

export default AccountLink;

AccountLink.propTypes = {
    children: arrayOf(node).isRequired,
    classes: shape({
        root: string,
        content: string,
        icon: string,
        text: string,
        root_normalPriority: string
    }),
    onClick: func
};
