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
import ReactDOM from 'react-dom';
import { User as UserIcon } from 'react-feather';

import { useUserContext } from '../../context/UserContext';
import Icon from '../Icon';
import AccountIconText from '../AccountIconText';

import classes from './accountTrigger.css';

const parentEl = document.querySelector('.header__accountTrigger');

const AccountTrigger = () => {
    const [{ isAccountDropdownOpen }, { dispatch }] = useUserContext();

    const iconColor = 'rgb(var(--venia-text))';
    const svgAttributes = {
        stroke: iconColor
    };

    const button = (
        <button
            className={classes.root}
            aria-label="Toggle account dropdown"
            onClick={() => dispatch({ type: 'toggleAccountDropdown', toggle: !isAccountDropdownOpen })}>
            <Icon src={UserIcon} attrs={svgAttributes} />
            <AccountIconText />
        </button>
    );

    return ReactDOM.createPortal(button, parentEl);
};

AccountTrigger.propTypes = {
    children: PropTypes.node,
    classes: PropTypes.shape({
        root: PropTypes.string
    })
};

export default AccountTrigger;
