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

/**
 * The Form component is similar to Flow in that it renders either the overview
 * or the editable form based on the 'editing' state value.
 */
const Form = props => {
    const { cart } = props;
    const [editing, setEditing] = useState(null);
    const hasShippingAddress = cart.shipping_addresses && cart.shipping_addresses.length > 0;
    const [shippingAddress, setShippingAddress] = useState(cart.shipping_addresses[0]);
    const child = editing ? (
        <EditableForm editing={editing} setEditing={setEditing} {...props} />
    ) : (
        <Overview
            classes={classes}
            {...props}
            setEditing={setEditing}
            shippingAddress={shippingAddress}
            hasShippingAddress={hasShippingAddress}
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
