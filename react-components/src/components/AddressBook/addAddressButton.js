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
import { PlusSquare as PlusIcon } from 'react-feather';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import { useUserContext } from '../../context/UserContext';

import classes from './addAddressButton.css';

const AddAddressButton = props => {
    const { displayType } = props;
    const [, { dispatch }] = useUserContext();

    const [t] = useTranslation('account');

    const rootClass = displayType ? classes[displayType] : classes.root;

    return (
        <button className={rootClass} aria-label="Add an address" onClick={() => dispatch({ type: 'openAddressForm' })}>
            <span className={classes.icon}>
                <PlusIcon size={18} />
            </span>
            <span className={classes.label}>{t('account:add-an-address', 'Add an address')}</span>
        </button>
    );
};

AddAddressButton.propTypes = {
    displayType: PropTypes.string
};

export default AddAddressButton;
