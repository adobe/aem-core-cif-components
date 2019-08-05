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
import React, { Component } from 'react';
import { func, object, oneOf, shape, string } from 'prop-types';
import HeartIcon from 'react-feather/dist/icons/heart';
import Edit2Icon from 'react-feather/dist/icons/edit-2';
import TrashIcon from 'react-feather/dist/icons/trash';
import classes from './section.css';

const SectionIcons = {
    Heart: HeartIcon,
    Edit2: Edit2Icon,
    Trash: TrashIcon
};

class Section extends Component {
    static propTypes = {
        classes: shape({
            menuItem: string,
            text: string
        }),
        icon: oneOf(['Heart', 'Edit2', 'Trash']),
        iconAttributes: object,
        onClick: func,
        text: string
    };

    get Icon() {
        const { icon } = this.props;
        const defaultAttributes = {
            color: 'rgb(var(--venia-teal))',
            width: '14px',
            height: '14px'
        };

        return icon ? SectionIcons[icon] : null;
    }

    render() {
        const { Icon } = this;
        const { onClick, text } = this.props;
        return (
            <li className={classes.menuItem}>
                <button onClick={onClick}>
                    <Icon />
                    <span className={classes.text}>{text}</span>
                </button>
            </li>
        );
    }
}

export default Section;
