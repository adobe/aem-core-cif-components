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

import React, { useCallback, useState, useRef } from 'react';
import { shape, string, node } from 'prop-types';
import { MoreVertical } from 'react-feather';

import classes from './kebab.css';
import Icon from '../Icon';
import { useEventListener } from '../../utils/hooks';

const Kebab = props => {
    const { children } = props;

    const [open, isOpen] = useState(false);
    const kebabRef = useRef(null);

    const handleOutsideKebabClick = useCallback(event => {
        if (!kebabRef.current.contains(event.target)) {
            isOpen(false);
        }
    }, []);

    useEventListener(document, 'mousedown', handleOutsideKebabClick);
    useEventListener(document, 'touchend', handleOutsideKebabClick);

    const handleKebabClick = useCallback(() => {
        isOpen(!open);
    }, [isOpen]);

    const toggleClass = open ? classes.dropdown_active : classes.dropdown;

    return (
        <div className={classes.root}>
            <button className={classes.kebab} onClick={handleKebabClick} ref={kebabRef}>
                <Icon src={MoreVertical} />
            </button>
            <ul className={toggleClass}>{children}</ul>
        </div>
    );
};

Kebab.propTypes = {
    classes: shape({
        dropdown: string,
        dropdown_active: string,
        kebab: string,
        root: string
    }),
    children: node
};
export default Kebab;
