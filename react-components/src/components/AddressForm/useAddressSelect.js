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
import { useTranslation } from 'react-i18next';
import { useAddressForm } from './useAddressForm';
import { useCheckoutState } from '../Checkout/checkoutContext';
import { useUserContext } from '../../context/UserContext';

export const useAddressSelect = (props = { initialAddress: undefined }) => {
    const { initialAddress } = props;

    const { findSavedAddress, getNewAddress, parseAddress, parseAddressFormValues } = useAddressForm();
    const [, dispatch] = useCheckoutState();
    const [{ currentUser }] = useUserContext();

    const parseInitialAddressSelectValue = address => {
        let initialValue = 0; // '0' is the value of option item 'New Address' in address select
        const foundAddress = findSavedAddress(address);
        return foundAddress ? foundAddress.id : initialValue;
    };
    const initialAddressId = initialAddress ? parseInitialAddressSelectValue(initialAddress) : 0;
    const { selectedAddressId, setSelectedAddressId } = useState(initialAddressId);

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

    const handleChangeAddressSelectInCheckout = (value, addressFormApi) => {
        const newAddressItemValue = 0;
        setSelectedAddressId(value);
        if (newAddressItemValue == value) {
            // clear the values of the form fields if the current select option item is 'New Address'
            addressFormApi.setValues(getNewAddress());
            dispatch({ type: 'setIsEditingNewAddress', editing: true });
            return;
        }

        const address = currentUser.addresses.find(address => address.id == value);
        // parse the "GraphQL format" address and normalize the properties (i.e. add the region_code on the same level as the rest of the props)
        const parsedAddress = parseAddress(address);
        // update form values by using formApi passed from <AddressForm> component, this is due to the fact that
        // this is the proposed way to update values of fields in 'informed' form library
        const parsedFormValues = parseAddressFormValues(parsedAddress);
        addressFormApi.setValues(parsedFormValues);
    };

    return {
        addressSelectItems,
        selectedAddressId,
        handleChangeAddressSelectInCheckout,
        parseInitialAddressSelectValue
    };
};
