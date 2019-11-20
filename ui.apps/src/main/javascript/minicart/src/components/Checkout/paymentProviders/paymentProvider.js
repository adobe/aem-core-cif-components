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
import React from 'react';
import { Text, useFormState } from 'informed';

import Braintree from './braintree';

import classes from './paymentProvider.css';

const PaymentProvider = () => {
    const formState = useFormState();
    let child;

    console.log(formState);

    const nonceValidation = (value, values) => {
        const nonce = value;
        const paymentMethod = values.payment_method;
        switch (paymentMethod) {
            case 'braintree': {
                return !nonce || nonce.length < 11 ? 'Please provide your credit card details.' : undefined;
            }
            case 'braintree_paypal': {
                return !nonce || nonce.length < 11 ? 'Please provide your PayPal details.' : undefined;
            }
            default: {
                // No nonce needed for any other payment methods
                return undefined;
            }
        }
    };

    switch (formState.values.payment_method) {
        case 'braintree': {
            child = <Braintree accept="card" setIsReady={() => {}} />;
            break;
        }

        case 'braintree_paypal': {
            child = <Braintree accept="paypal" setIsReady={() => {}} />;
            break;
        }

        default: {
            return null;
        }
    }

    return (
        <div className={classes.braintree}>
            {child}
            <Text type="hidden" field="payment_nonce" validate={nonceValidation} />
            {formState.errors.payment_nonce && (
                <p className={classes.error_message}>{formState.errors.payment_nonce}</p>
            )}
        </div>
    );
};

export default PaymentProvider;
