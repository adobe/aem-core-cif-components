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
import { func, oneOf, shape, string, bool } from 'prop-types';
import { Heart, Edit2, Trash } from 'react-feather';

import classes from './section.css';
import Icon from '../Icon';

const defaultIconAttributes = {
    color: 'rgb(var(--venia-teal))',
    width: '14px',
    height: '14px'
};
const filledIconAttributes = {
    ...defaultIconAttributes,
    fill: 'rgb(var(--venia-teal))'
};
const icons = {
    Heart,
    Edit2,
    Trash
};

const Section = props => {
    const { icon, isFilled, onClick, text } = props;
    const attributes = isFilled ? filledIconAttributes : defaultIconAttributes;

    const iconSrc = icons[icon];
    return (
        <li className={classes.menuItem}>
            <button onMouseDown={onClick}>
                {iconSrc && <Icon src={iconSrc} attrs={attributes} />}
                <span className={classes.text}>{text}</span>
            </button>
        </li>
    );
};

Section.propTypes = {
    classes: shape({
        menuItem: string,
        text: string
    }),
    icon: oneOf(Object.keys(icons)),
    isFilled: bool,
    onClick: func,
    text: string
};

export default Section;
