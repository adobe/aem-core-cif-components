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
import { shape, string } from 'prop-types';

import Overview from './overview';
import classes from './form.css';
import EditableForm from './editableForm';
import isObjectEmpty from '../../utils/isObjectEmpty';

/**
 * The Form component is similar to Flow in that it renders either the overview
 * or the editable form based on the 'editing' state value.
 */
const Form = props => {
    const {
        cart: { shipping_addresses = [], selected_payment_method = undefined, billing_address = undefined }
    } = props;
    const [editing, setEditing] = useState(null);

    const hasShippingAddress =
        shipping_addresses && shipping_addresses.length > 0 && shipping_addresses[0].firstname !== null;
    const actualAddress = hasShippingAddress
        ? {
              ...shipping_addresses[0],
              region_code: shipping_addresses[0].region.code,
              country: shipping_addresses[0].country.code
          }
        : {};

    const [shippingAddress, setShippingAddress] = useState(actualAddress);

    const hasPaymentMethod = selected_payment_method && selected_payment_method.code.length > 0;
    const initialPaymentMethod = hasPaymentMethod ? selected_payment_method : {};
    const [paymentData, setPaymentData] = useState(initialPaymentMethod);

    let flatBillingAddress =
        billing_address && billing_address.city !== null
            ? {
                  ...billing_address,
                  region_code: billing_address.region.code,
                  country: billing_address.country.code
              }
            : {};
    const [cartBillingAddress, setBillingAddress] = useState(flatBillingAddress);
    console.log(`Billing address is `, cartBillingAddress);

    let availableShippingMethods;
    let selectedShippingMethod;

    if (!isObjectEmpty(shippingAddress)) {
        availableShippingMethods = shippingAddress.available_shipping_methods;
        selectedShippingMethod = shippingAddress.selected_shipping_method;
        console.log(`Shipping methods available`, availableShippingMethods);
    } else {
        availableShippingMethods = [];
        selectedShippingMethod = {};
    }

    const [shippingMethod, setShippingMethod] = useState(selectedShippingMethod);
    console.log(`Selected shipping method`, shippingMethod);

    const child = editing ? (
        <EditableForm
            editing={editing}
            setEditing={setEditing}
            shippingAddress={shippingAddress}
            setShippingAddress={setShippingAddress}
            initialPaymentMethod={paymentData}
            setPaymentData={setPaymentData}
            billingAddress={cartBillingAddress}
            setBillingAddress={setBillingAddress}
            availableShippingMethods={availableShippingMethods}
            shippingMethod={shippingMethod}
            setShippingMethod={setShippingMethod}
            {...props}
        />
    ) : (
        <Overview
            classes={classes}
            setEditing={setEditing}
            shippingAddress={shippingAddress}
            hasShippingAddress={!isObjectEmpty(shippingAddress)}
            paymentData={{ details: paymentData.title }}
            hasPaymentMethod={!isObjectEmpty(paymentData)}
            hasShippingMethod={!isObjectEmpty(shippingMethod)}
            shippingMethod={shippingMethod}
            {...props}
        />
    );
    return <div className={classes.root}>{child}</div>;
};

Form.propTypes = {
    classes: shape({
        root: string
    })
};

export default Form;
