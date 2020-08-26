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

import { useUserContext } from '../../context/UserContext';
import AddressItem from './addressItem';
import AddAddressButton from './addAddressButton';

import classes from './addressItemsContainer.css';

const AddressItems = props => {
    const { displayType } = props;
    const [{ currentUser }] = useUserContext();

    const rootClass = displayType ? classes[displayType] : classes.root;

    return (
        <div className={rootClass}>
            {currentUser.addresses.length > 0 &&
                currentUser.addresses.map(address => (
                    <AddressItem key={address.id} address={address} displayType={displayType} />
                ))}
            <AddAddressButton displayType={displayType} />
        </div>
    );
};

AddressItems.propTypes = {
    displayType: PropTypes.string
};

export default AddressItems;
