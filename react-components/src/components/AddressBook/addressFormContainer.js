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

import { useUserContext } from '../../context/UserContext';
import { useAddressForm } from '../AddressForm/useAddressForm';
import AddressForm from '../AddressForm';

import classes from './addressFormContainer.css';
import { useTranslation } from 'react-i18next';

const AddressFormContainer = () => {
    const [{ isShowAddressForm }] = useUserContext();
    const { countries, handleSubmit, handleCancel, errorMessage, updateAddress } = useAddressForm();
    const [t] = useTranslation(['account', 'common']);

    return (
        <>
            <div className={isShowAddressForm ? classes.mask_active : classes.mask}></div>
            {isShowAddressForm && (
                <div className={classes.container}>
                    <AddressForm
                        cancel={handleCancel}
                        countries={countries}
                        formErrorMessage={errorMessage}
                        initialValues={updateAddress}
                        showDefaultAddressCheckbox={true}
                        submit={handleSubmit}
                        formHeading={t('account:address-form-heading', 'Address')}
                        submitButtonLabel={t('common:save', 'Save')}
                    />
                </div>
            )}
        </>
    );
};

export default AddressFormContainer;
