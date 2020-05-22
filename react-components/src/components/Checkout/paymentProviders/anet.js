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
import React, { useEffect, useState, Fragment } from 'react';

import { useCheckoutState } from '../checkoutContext';
import { oneOf } from 'prop-types';

import Field from '../../Field';
import TextInput from '../../TextInput';
import Select from '../../Select';

import { isRequired } from '../../../utils/formValidators';

const Anet = props => {
    const [{ anetActive }, dispatch] = useCheckoutState();

    const expMonths = [
        {
            label: 'Month',
            value: 'Month'
        },
        {
            label: '1',
            value: '1'
        },
        {
            label: '2',
            value: '2'
        },
        {
            label: '3',
            value: '3'
        },
        {
            label: '4',
            value: '4'
        },
        {
            label: '5',
            value: '5'
        },
        {
            label: '6',
            value: '6'
        },
        {
            label: '7',
            value: '7'
        },
        {
            label: '8',
            value: '8'
        },
        {
            label: '9',
            value: '9'
        },
        {
            label: '10',
            value: '10'
        },
        {
            label: '11',
            value: '11'
        },
        {
            label: '12',
            value: '12'
        }]
    const expYears = [
        {
            label: 'Year',
            value: 'Year'
        }
    ]

    let d = new Date();
    let n = d.getFullYear();

    for (let i = 0; i < 15; i++) {
        let j = (n + i).toString();
        expYears.push({ label: j, value: j.slice(-2) });
    }

    // activate accept.js if its inactive
    // TODO remove accept.js when they leave this
    useEffect(() => {
        if (!anetActive) {
            console.log("load accept.js");
            // TODO update to use either sandbox or prod
            // loadScript('https://js.authorize.net/v1/Accept.js');
            loadScript('https://jstest.authorize.net/v1/Accept.js');
            return;
        }
    }, [anetActive, props.accept]);

    return (
        <Fragment>
            <Field label="Card Number">
                <TextInput id="cardNumber" field="cardNumber" validate={isRequired} />
            </Field>
            <Field label="CSV">
                <TextInput id="cardCode" field="cardCode" validate={isRequired} />
            </Field>
            <Field label="Exp Month">
                <Select items={expMonths} id="expMonth" field="expMonth" validate={isRequired} />
            </Field>
            <Field label="Exp Year">
                <Select items={expYears} id="expYear" field="expYear" validate={isRequired} />
            </Field>
        </Fragment>
    );
};

function loadScript(src) {
    var tag = document.createElement('script');
    tag.src = src;
    document.body.appendChild(tag);
}

Anet.propTypes = {
    accept: oneOf(['card']).isRequired
};

export default Anet;
