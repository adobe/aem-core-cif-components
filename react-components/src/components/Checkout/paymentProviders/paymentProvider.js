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
import Anet from './anet';
import nonceValidation from './nonceValidation';
import { useCheckoutState } from '../checkoutContext';
import { useFieldApi } from 'informed';

import classes from './paymentProvider.css';

const PaymentProvider = () => {
    const formState = useFormState();
    const [{ anetToken, anetApiId }, dispatch] = useCheckoutState();
    const paymentNonceField = useFieldApi('payment_nonce');
    const dataDescriptorField = useFieldApi('dataDescriptor');
    const ccLast4Field = useFieldApi('ccLast4');
    const ccType = useFieldApi('ccType');

    let child;

    switch (formState.values.payment_method) {
        case 'braintree': {
            console.log("braintree pp");
            child = <Braintree accept="card" />;
            break;
        }

        case 'braintree_paypal': {
            console.log("braintreepal pp");
            child = <Braintree accept="paypal" />;
            break;
        }

        case 'authnetcim': {
            console.log("anet pp", formState);
            child = <Anet accept="card" />;
            break;
        }

        default: {
            return null;
        }
    }

    return (
        <div className={classes.braintree}>
            {child}
            <Text type="hidden" field="dataDescriptor" />
            <Text type="hidden" field="ccLast4" />
            <Text type="hidden" field="ccType" />
            <Text type="hidden" field="payment_nonce" validate={nonceValidation} />
            <Text type="hidden" field="anetError" />
            {formState.errors.payment_nonce && (
                <p className={classes.error_message}>{formState.errors.payment_nonce}</p>
            )}
        </div>
    );
};

export default PaymentProvider;
