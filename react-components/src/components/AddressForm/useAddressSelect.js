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
import { useTranslation } from 'react-i18next';

import { useAddressForm } from './useAddressForm';
import { useUserContext } from '../../context/UserContext';

export const useAddressSelect = () => {
    const { getNewCheckoutShippingAddress, parseAddress, parseAddressFormValues } = useAddressForm();
    const [{ currentUser }] = useUserContext();

    const [t] = useTranslation('checkout');

    const addressSelectNewAddressItem = {
        label: t('checkout:address-form-address-select-new-address', 'New Address'),
        value: 0
    };
    const addressSelectItems = currentUser.addresses.map(address => {
        return {
            label: address.street.join(' '),
            value: address.id
        };
    });
    addressSelectItems.unshift(addressSelectNewAddressItem);

    const handleChangeCheckoutShippingAddressSelect = (value, addressFormApi) => {
        const newAddressItemValue = 0;
        if (newAddressItemValue == value) {
            // clear the values of the form fields if the current select option item is 'New Address'
            addressFormApi.setValues(getNewCheckoutShippingAddress());
            return;
        }

        const address = currentUser.addresses.find(address => address.id == value);
        const parsedAddress = parseAddress(address);
        // update form values by using formApi passed from <AddressForm> component, this is due to the fact that
        // this is the proposed way to update values of fields in 'informed' form library
        addressFormApi.setValues(parseAddressFormValues(parsedAddress));
    };

    const parseInitialAddressSelectValue = address => {
        let initialValue = null;
        if (address && currentUser.addresses.length > 0) {
            const foundAddress = currentUser.addresses.find(item => item.street.join(' ') === address.street.join(' '));
            const newAddressItemValue = 0; // this is the value of option item 'New Address' in address select
            initialValue = foundAddress ? foundAddress.id : newAddressItemValue;
        }
        return initialValue;
    };

    return {
        addressSelectItems,
        handleChangeCheckoutShippingAddressSelect,
        parseInitialAddressSelectValue
    };
};
