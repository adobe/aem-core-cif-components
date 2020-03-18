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
import React, { useCallback } from 'react';
import { Form } from 'informed';
import { array, bool, func, shape, string, object } from 'prop-types';
import { useTranslation } from 'react-i18next';

import Button from '../Button';
import Select from '../Select';

import classes from './shippingForm.css';

const ShippingForm = props => {
    const [t] = useTranslation(['checkout', 'common']);
    const { availableShippingMethods, cancel, shippingMethod, submit, submitting } = props;
    let initialValue;
    let selectableShippingMethods;

    if (availableShippingMethods.length > 0) {
        selectableShippingMethods = availableShippingMethods
            .filter(method => method.carrier_code && method.carrier_title)
            .map(({ carrier_code, carrier_title }) => ({
                label: carrier_title,
                value: carrier_code
            }));
        initialValue = shippingMethod ? shippingMethod.carrier_code : availableShippingMethods[0].carrier_code;
    } else {
        selectableShippingMethods = [];
        initialValue = '';
    }

    const handleSubmit = useCallback(
        ({ shippingMethod }) => {
            const selectedShippingMethod = availableShippingMethods.find(
                ({ carrier_code }) => carrier_code === shippingMethod
            );

            if (!selectedShippingMethod) {
                console.warn(
                    `Could not find the selected shipping method ${selectedShippingMethod} in the list of available shipping methods.`
                );
                cancel();
                return;
            }
            submit({ shippingMethod: selectedShippingMethod });
        },
        [availableShippingMethods, cancel, submit]
    );

    return (
        <Form className={classes.root} onSubmit={handleSubmit}>
            <div className={classes.body}>
                <h2 className={classes.heading}>{t('checkout:shipping-information', 'Shipping Information')}</h2>
                <div className={classes.shippingMethod} id={classes.shippingMethod}>
                    <label htmlFor={classes.shippingMethod}>{t('checkout:shipping-method', 'Shipping Method')}</label>
                    <Select field="shippingMethod" initialValue={initialValue} items={selectableShippingMethods} />
                </div>
            </div>
            <div className={classes.footer}>
                <Button onClick={cancel}>{t('common:cancel', 'Cancel')}</Button>
                <Button priority="high" type="submit" disabled={submitting}>
                    {t('checkout:use-shipping-method', 'Use Method')}
                </Button>
            </div>
        </Form>
    );
};

ShippingForm.propTypes = {
    availableShippingMethods: array.isRequired,
    cancel: func.isRequired,
    classes: shape({
        body: string,
        button: string,
        footer: string,
        heading: string,
        shippingMethod: string
    }),
    shippingMethod: object,
    submit: func.isRequired,
    submitting: bool
};

ShippingForm.defaultProps = {
    availableShippingMethods: [{}]
};

export default ShippingForm;
