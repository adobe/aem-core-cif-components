/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
import React from 'react';
import { PlusSquare as PlusIcon } from 'react-feather';
import { useIntl } from 'react-intl';
import PropTypes from 'prop-types';

import { useUserContext } from '../../context/UserContext';

import classes from './addAddressButton.css';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const AddAddressButton = props => {
    const { displayType } = props;
    const [, { dispatch }] = useUserContext();
    const intl = useIntl();
    const rootClass = displayType ? classes[displayType] : classes.root;

    return (
        <button className={rootClass} aria-label="Add an address" onClick={() => dispatch({ type: 'openAddressForm' })}>
            <span className={classes.icon}>
                <PlusIcon size={18} />
            </span>
            <span className={classes.label}>
                {intl.formatMessage({ id: 'account:add-an-address', defaultMessage: 'Add an address' })}
            </span>
        </button>
    );
};

AddAddressButton.propTypes = {
    displayType: PropTypes.string
};

export default AddAddressButton;
