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
        cart: { shipping_addresses = [], selected_payment_method = {} }
    } = props;
    const [editing, setEditing] = useState(null);

    const hasShippingAddress = shipping_addresses && shipping_addresses.length > 0;
    const actualAddress = hasShippingAddress
        ? {
              ...shipping_addresses[0],
              region_code: shipping_addresses[0].region.code,
              country: shipping_addresses[0].country.code
          }
        : {};

    const [shippingAddress, setShippingAddress] = useState(actualAddress);

    const hasPaymentMethod = !isObjectEmpty(selected_payment_method);

    const [paymentData, setPaymentData] = useState({ details: { cardType: selected_payment_method.title } });
    const child = editing ? (
        <EditableForm
            editing={editing}
            setEditing={setEditing}
            setShippingAddress={setShippingAddress}
            shippingAddress={shippingAddress}
            setPaymentData={setPaymentData}
            {...props}
        />
    ) : (
        <Overview
            classes={classes}
            {...props}
            setEditing={setEditing}
            shippingAddress={shippingAddress}
            hasShippingAddress={hasShippingAddress}
            paymentData={paymentData}
            hasPaymentMethod={hasPaymentMethod}
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
