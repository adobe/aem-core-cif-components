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
import PropTypes from 'prop-types';
import { User as UserIcon } from 'react-feather';

import { useUserContext } from '../../context/UserContext';
import Icon from '../Icon';

import classes from './accountTrigger.css';

const AccountTrigger = props => {
    const [{ isAccountDropdownOpen }, { toggleAccountDropdown }] = useUserContext();
    const { label } = props;

    const iconColor = 'rgb(var(--venia-text))';
    const svgAttributes = {
        stroke: iconColor
    };

    return (
        <button
            className={classes.root}
            aria-label="Toggle account dropdown"
            onClick={() => toggleAccountDropdown(!isAccountDropdownOpen)}>
            <Icon src={UserIcon} attrs={svgAttributes} />
            {label && <span className={classes.label}>{label}</span>}
        </button>
    );
};

AccountTrigger.propTypes = {
    children: PropTypes.node,
    classes: PropTypes.shape({
        root: PropTypes.string
    }),
    label: PropTypes.oneOfType([PropTypes.object, PropTypes.string])
};

export default AccountTrigger;
