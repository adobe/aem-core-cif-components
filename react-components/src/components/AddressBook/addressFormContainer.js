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
import AddressForm from '../AddressForm';

import classes from './addressFormContainer.css';

const AddressFormContainer = () => {
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
        try {
            if (updateAddress) {
                const { data } = await updateCustomerAddress({
                    variables: {
                        id: updateAddress.id,
                        country_code: 'US',
                        region: {
                            region_code: formValues.region_code,
                            region_id: getRegionId(countries, 'US', formValues.region_code)
                        },
                        default_billing: formValues.default_shipping,
                        ...formValues
                    }
                });
                dispatch({ type: 'updateAddresses', address: data.updateCustomerAddress });
                dispatch({ type: 'endEditingAddress' });
            } else {
                const { data } = await createCustomerAddress({
                    variables: {
                        country_code: 'US',
                        region: {
                            region_code: formValues.region_code,
                            region_id: getRegionId(countries, 'US', formValues.region_code)
                        },
                        default_billing: formValues.default_shipping,
                        ...formValues
                    }
                });
                dispatch({ type: 'setNewAddress', address: data.createCustomerAddress });
            }
            dispatch({ type: 'closeAddressForm' });
        } catch (error) {
            dispatch({ type: 'setAddressFormError', error: error.toString() });
        }
    };

    const getRegionId = (countries, countryCode, regionCode) => {
        const region =
            countries &&
            countries
                .filter(country => country.id == countryCode && country.available_regions)
                .map(country => country.available_regions.find(region => region.code == regionCode));
        return region ? region[0].id : null;
    };

    return (
        <>
            <div className={isShowAddressForm ? classes.mask_active : classes.mask}></div>
            {isShowAddressForm && (
                <div className={classes.container}>
                    <AddressForm
                        cancel={handleCancel}
                        countries={countries}
                        formErrorMessage={addressFormError}
                        initialValues={updateAddress}
                        showDefaultAddressCheckbox={true}
                        submit={handleSubmit}
                    />
                </div>
            )}
        </>
    );
};

export default AddressFormContainer;
