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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation } from '@apollo/react-hooks';

import MUTATION_UPDATE_CUSTOMER_ADDRESS from '../../queries/mutation_update_customer_address.graphql';
import MUTATION_CREATE_CUSTOMER_ADDRESS from '../../queries/mutation_create_customer_address.graphql';
import { useCountries } from '../../utils/hooks';
import { useUserContext } from '../../context/UserContext';
import LoadingIndicator from '../LoadingIndicator';
import AddressForm from '../Checkout/addressForm';

import classes from './addressFormContainer.css';

const AddressFormContainer = () => {
    const [submitting, setIsSubmitting] = useState(false);
    const [{ isShowAddressForm, addressFormError, updateAddress }, { dispatch }] = useUserContext();
    const { countries } = useCountries();
    const [createCustomerAddress] = useMutation(MUTATION_CREATE_CUSTOMER_ADDRESS);
    const [updateCustomerAddress] = useMutation(MUTATION_UPDATE_CUSTOMER_ADDRESS);

    const [t] = useTranslation('account');

    const handleCancel = () => {
        dispatch({ type: 'clearAddressFormError' });
        dispatch({ type: 'closeAddressForm' });
        if (updateAddress) {
            dispatch({ type: 'endEditingAddress' });
        }
    };

    const handleSubmit = async formValues => {
        setIsSubmitting(true);
        try {
            if (updateAddress) {
                const { data } = await updateCustomerAddress({
                    variables: { id: updateAddress.id, region: { region_code: formValues.region_code }, ...formValues }
                });
                dispatch({ type: 'updateAddresses', address: data.updateCustomerAddress });
                dispatch({ type: 'endEditingAddress' });
            } else {
                const { data } = await createCustomerAddress({
                    variables: { country_code: 'US', region: { region_code: formValues.region_code }, ...formValues }
                });
                dispatch({ type: 'setNewAddress', address: data.createCustomerAddress });
            }
            dispatch({ type: 'closeAddressForm' });
        } catch (error) {
            dispatch({ type: 'setAddressFormError', error: error.toString() });
        }
        setIsSubmitting(false);
    };

    return (
        <>
            <div className={isShowAddressForm ? classes.mask_active : classes.mask}></div>
            {isShowAddressForm && (
                <div className={classes.container}>
                    {submitting && (
                        <LoadingIndicator>{t('account:address-form-submitting', 'Submitting...')}</LoadingIndicator>
                    )}
                    <AddressForm
                        cancel={handleCancel}
                        countries={countries}
                        initialValues={updateAddress}
                        showDefaultAddressCheckbox={true}
                        submit={handleSubmit}
                        submitLabel={t('account:address-save', 'Save')}
                    />
                    {addressFormError && <div className={classes.error}>{addressFormError}</div>}
                </div>
            )}
        </>
    );
};

export default AddressFormContainer;
