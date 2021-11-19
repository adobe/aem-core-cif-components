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
import { Edit as EditIcon, Trash2 as DeleteIcon } from 'react-feather';
import { useIntl } from 'react-intl';
import PropTypes from 'prop-types';

import { useUserContext } from '../../context/UserContext';
import AddressDeleteModal from './addressDeleteModal';

import classes from './addressItem.css';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const AddressItem = props => {
    const { address, displayType } = props;
    const [{ deleteAddress }, { dispatch }] = useUserContext();
    const rootClass = displayType ? classes[displayType] : classes.root;
    const street = address.street.join(' ');
    const intl = useIntl();

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
                <button
                    className={classes.editButton}
                    onClick={() => dispatch({ type: 'beginEditingAddress', address })}>
                    <span className={classes.icon}>
                        <EditIcon size={18} />
                    </span>
                    <span className={classes.label}>
                        {intl.formatMessage({ id: 'account:address-edit', defaultMessage: 'Edit' })}
                    </span>
                </button>
                {!address.default_shipping && !address.default_billing && (
                    <button
                        className={classes.deleteButton}
                        onClick={() => dispatch({ type: 'beginDeletingAddress', address })}>
                        <span className={classes.icon}>
                            <DeleteIcon size={18} />
                        </span>
                        <span className={classes.label}>
                            {intl.formatMessage({ id: 'account:address-delete', defaultMessage: 'Delete' })}
                        </span>
                    </button>
                )}
                {!!address.default_shipping && !!address.default_billing && (
                    <div className={classes.defaultTag}>
                        <span>
                            {intl.formatMessage({ id: 'account:address-default-tag', defaultMessage: 'Default' })}
                        </span>
                    </div>
                )}
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
