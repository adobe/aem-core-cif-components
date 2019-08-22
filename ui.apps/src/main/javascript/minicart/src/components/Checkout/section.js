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
import { Edit2 as EditIcon } from 'react-feather';

import Icon from '../Icon';
import classes from './section.css';

const editIconAttrs = {
    color: 'black',
    width: 18
};
const EDIT_ICON = <Icon src={EditIcon} attrs={editIconAttrs} />;

const Section = props => {
    const { children, label, showEditIcon = false, ...restProps } = props;

    const icon = showEditIcon ? EDIT_ICON : null;

    return (
        <button className={classes.root} {...restProps}>
            <span className={classes.content}>
                <span className={classes.label}>
                    <span>{label}</span>
                </span>
                <span className={classes.summary}>{children}</span>
                <span className={classes.icon}>{icon}</span>
            </span>
        </button>
    );
};

Section.propTypes = {
    classes: shape({
        content: string,
        icon: string,
        label: string,
        root: string,
        summary: string
    }),
    label: node,
    showEditIcon: bool
};

export default Section;
