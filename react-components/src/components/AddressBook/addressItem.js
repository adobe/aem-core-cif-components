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
import { Edit as EditIcon, Trash2 as DeleteIcon } from 'react-feather';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import { useUserContext } from '../../context/UserContext';
import AddressDeleteModal from './addressDeleteModal';

import classes from './addressItem.css';

const AddressItem = props => {
    const { address, displayType } = props;
    const [{ deleteAddress }, { dispatch }] = useUserContext();

    const [t] = useTranslation('account');

    const rootClass = displayType ? classes[displayType] : classes.root;
    const street = address.street.join(' ');

    const handleEditClick = () => {
        dispatch({ type: 'beginEditingAddress', address });
        dispatch({ type: 'openAddressForm' });
    };

    const handleDeleteClick = () => {
        dispatch({ type: 'beginDeletingAddress', address });
    };

    return (
        <div className={rootClass}>
            <div className={classes.summary}>
                <div className={classes.name}>
                    <strong>
                        {address.firstname} {address.lastname}
                    </strong>
                </div>
                <div className={classes.street}>{street}</div>
                <div className={classes.cityRegion}>
                    {address.city}, {address.region.region_code}
                </div>
                <div className={classes.country}>{address.country_code}</div>
            </div>
            <div className={classes.actions}>
                <button className={classes.editButton} onClick={handleEditClick}>
                    <span className={classes.icon}>
                        <EditIcon size={18} />
                    </span>
                    <span className={classes.label}>{t('account:address-edit', 'Edit')}</span>
                </button>
                <button className={classes.deleteButton} onClick={handleDeleteClick}>
                    <span className={classes.icon}>
                        <DeleteIcon size={18} />
                    </span>
                    <span className={classes.label}>{t('account:address-delete', 'Delete')}</span>
                </button>
            </div>
            {deleteAddress && deleteAddress.id === address.id && <AddressDeleteModal />}
        </div>
    );
};

AddressItem.propTypes = {
    address: PropTypes.object,
    displayType: PropTypes.string
};

export default AddressItem;
