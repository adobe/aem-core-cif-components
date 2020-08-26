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
import { useTranslation } from 'react-i18next';

import { useUserContext } from '../../context/UserContext';
import Button from '../Button';

import classes from './addressDeleteModal.css';

const AddressDeleteModal = () => {
    const [{ deleteAddress: address }, { deleteAddress, dispatch }] = useUserContext();

    const [t] = useTranslation('account');

    return (
        <div className={classes.root}>
            <div className={classes.cancel}>
                <Button priority="normal" type="button" onClick={() => dispatch({ type: 'endDeletingAddress' })}>
                    {t('account:address-delete-cancel', 'Cancel')}
                </Button>
            </div>
            <div className={classes.delete}>
                <Button
                    priority="high"
                    type="button"
                    onClick={() => {
                        deleteAddress(address);
                    }}>
                    {t('account:address-delete-confirm', 'Delete')}
                </Button>
            </div>
        </div>
    );
};

export default AddressDeleteModal;
