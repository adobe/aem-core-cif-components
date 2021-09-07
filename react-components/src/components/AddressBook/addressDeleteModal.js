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
import { useIntl } from 'react-intl';

import { useUserContext } from '../../context/UserContext';
import Button from '../Button';

import classes from './addressDeleteModal.css';

const AddressDeleteModal = () => {
    const [{ deleteAddress: address }, { deleteAddress, dispatch }] = useUserContext();
    const intl = useIntl();

    return (
        <div className={classes.root}>
            <div className={classes.cancel}>
                <Button priority="normal" type="button" onClick={() => dispatch({ type: 'endDeletingAddress' })}>
                    {intl.formatMessage({ id: 'account:address-delete-cancel', defaultMessage: 'Cancel' })}
                </Button>
            </div>
            <div className={classes.delete}>
                <Button
                    priority="high"
                    type="button"
                    onClick={() => {
                        deleteAddress(address);
                    }}>
                    {intl.formatMessage({ id: 'account:address-delete-confirm', defaultMessage: 'Delete' })}
                </Button>
            </div>
        </div>
    );
};

export default AddressDeleteModal;
