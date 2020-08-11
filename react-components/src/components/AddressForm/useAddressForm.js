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
import { useState } from 'react';
import { useUserContext } from '../../context/UserContext';
import { useMutation } from '@apollo/react-hooks';

import { useCountries } from '../../utils/hooks';
import { createAddress, updateAddress } from '../../actions/user';

import MUTATION_UPDATE_CUSTOMER_ADDRESS from '../../queries/mutation_update_customer_address.graphql';
import MUTATION_CREATE_CUSTOMER_ADDRESS from '../../queries/mutation_create_customer_address.graphql';

export const useAddressForm = () => {
    const [userState, { dispatch }] = useUserContext();
    const { countries } = useCountries();
    const [inProgress, setInProgress] = useState(false);

    const [createCustomerAddress] = useMutation(MUTATION_CREATE_CUSTOMER_ADDRESS);
    const [updateCustomerAddress] = useMutation(MUTATION_UPDATE_CUSTOMER_ADDRESS);

    let errorMessage = '';
    if (userState.addressFormError && userState.addressFormError.length > 0) {
        errorMessage = userState.addressFormError;
    }

    const getRegionId = (countries, countryCode, regionCode) => {
        const region =
            countries &&
            countries
                .filter(country => country.id == countryCode && country.available_regions)
                .map(country => country.available_regions.find(region => region.code == regionCode));
        return region && region[0] ? region[0].id : null;
    };

    const handleSubmit = async formValues => {
        setInProgress(true);
        const variables = {
            country_code: 'US',
            region: {
                region_code: formValues.region_code,
                region_id: getRegionId(countries, 'US', formValues.region_code)
            },
            default_billing: formValues.default_shipping,
            ...formValues
        };
        let resetFields;
        if (formValues.default_shipping === true) {
            resetFields = {
                default_shipping: false,
                default_billing: false
            };
        }

        if (userState.updateAddress) {
            variables.id = userState.updateAddress.id;
            updateAddress({ updateCustomerAddress, variables, resetFields, dispatch });
        } else {
            createAddress({ createCustomerAddress, variables, resetFields, dispatch });
        }
        setInProgress(false);
    };

    const handleCancel = () => {
        dispatch({ type: 'clearAddressFormError' });
        if (userState.updateAddress) {
            dispatch({ type: 'endEditingAddress' });
        }
        dispatch({ type: 'closeAddressForm' });
    };

    return {
        inProgress,
        countries,
        getRegionId,
        handleSubmit,
        handleCancel,
        errorMessage,
        updateAddress: userState.updateAddress
    };
};
