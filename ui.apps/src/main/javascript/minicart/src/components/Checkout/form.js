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
import React, { useEffect } from 'react';

import Overview from './overview';
import classes from './form.css';
import EditableForm from './editableForm';
import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';

const parseAddress = (address, email) => {
    let result = {
        ...address,
        region_code: address.region.code,
        country: address.country.code
    };
    if (email) {
        result.email = email;
    }
    return result;
};

/**
 * The Form component is similar to Flow in that it renders either the overview
 * or the editable form based on the 'editing' state value.
 */
const Form = () => {
    const [{ cart }] = useCartState();
    const [{ editing, shippingAddress }, dispatch] = useCheckoutState();
    const { shipping_addresses = [], selected_payment_method = undefined, billing_address = undefined, email } = cart;

    useEffect(() => {
        if (shipping_addresses && shipping_addresses.length > 0 && shipping_addresses[0].firstname !== null) {
            dispatch({
                type: 'setShippingAddress',
                shippingAddress: parseAddress(shipping_addresses[0], email)
            });
        }
    }, [shipping_addresses]);

    useEffect(() => {
        if (selected_payment_method && selected_payment_method.code.length > 0) {
            dispatch({ type: 'setPaymentMethod', paymentMethod: selected_payment_method });
        }
    }, [selected_payment_method]);

    useEffect(() => {
        if (billing_address && billing_address.city !== null) {
            dispatch({
                type: 'setBillingAddress',
                billingAddress: parseAddress(billing_address)
            });
        }
    }, [billing_address]);

    useEffect(() => {
        if (
            shippingAddress &&
            shippingAddress.selected_shipping_method &&
            shippingAddress.selected_shipping_method.carrier_code !== null
        ) {
            dispatch({ type: 'setShippingMethod', shippingMethod: shippingAddress.selected_shipping_method });
        }
    }, [shippingAddress]);

    const child = editing ? <EditableForm /> : <Overview classes={classes} />;
    return <div className={classes.root}>{child}</div>;
};

export default Form;
