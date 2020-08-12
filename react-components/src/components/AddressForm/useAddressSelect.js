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
import { useAddressForm } from './useAddressForm';
import { useCheckoutState } from '../Checkout/checkoutContext';
import { useUserContext } from '../../context/UserContext';

export const useAddressSelect = () => {
    const { parseAddress, parseAddressFormValues } = useAddressForm();
    const [, dispatch] = useCheckoutState();
    const [{ currentUser }] = useUserContext();

    const addressesItems = currentUser.addresses.map(address => {
        return {
            label: address.street.join(' '),
            value: address.id
        };
    });

    const handleChangeCheckoutShippingAddressSelect = (value, formApi) => {
        const address = currentUser.addresses.find(address => address.id == value);
        const parsedAddress = parseAddress(address, currentUser.email);
        dispatch({
            type: 'setShippingAddress',
            shippingAddress: parsedAddress
        });
        dispatch({ type: 'setEditing', editing: 'address' });
        // update form values by using formApi passed from <AddressForm> component, this is due to the fact that
        // this is the proposed way to update values of fields in 'informed' form library
        formApi.setValues(parseAddressFormValues(parsedAddress));
    };

    const parseInitialAddressSelectValue = address => {
        let initialValue = '';
        if (address) {
            const foundAddress = currentUser.addresses.find(item => item.street.join(' ') === address.street.join(' '));
            initialValue = foundAddress ? foundAddress.id : initialValue;
        }
        return initialValue;
    };

    return {
        addressesItems,
        handleChangeCheckoutShippingAddressSelect,
        parseInitialAddressSelectValue
    };
};
