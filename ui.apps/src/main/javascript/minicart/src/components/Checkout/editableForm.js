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
import { array, bool, func, object, oneOf, shape, string } from 'prop-types';
import { useCountries } from '../../utils/hooks';

import AddressForm from './addressForm';
// import PaymentsForm from './paymentsForm';
// import ShippingForm from './shippingForm';

/**
 * The EditableForm component renders the actual edit forms for the sections
 * within the form.
 */

const EditableForm = props => {
    const {
        editing,
        setEditing,
        submitPaymentMethodAndBillingAddress,
        submitShippingMethod,
        submitting,
        isAddressInvalid,
        invalidAddressMessage
    } = props;

    let countries = useCountries();

    const handleCancel = useCallback(() => {
        setEditing(null);
    }, [setEditing]);

    const handleSubmitAddressForm = useCallback(
        async formValues => {
            setEditing(null);
        },
        [setEditing]
    );

    const handleSubmitPaymentsForm = useCallback(
        async formValues => {
            await submitPaymentMethodAndBillingAddress({
                formValues
            });
            setEditing(null);
        },
        [setEditing, submitPaymentMethodAndBillingAddress]
    );

    const handleSubmitShippingForm = useCallback(
        async formValues => {
            await submitShippingMethod({
                formValues
            });
            setEditing(null);
        },
        [setEditing, submitShippingMethod]
    );

    switch (editing) {
        case 'address': {
            const { shippingAddress } = props;

            return (
                <AddressForm
                    cancel={handleCancel}
                    countries={countries}
                    isAddressInvalid={isAddressInvalid}
                    invalidAddressMessage={invalidAddressMessage}
                    initialValues={shippingAddress}
                    submit={handleSubmitAddressForm}
                    submitting={submitting}
                />
            );
        }
        // case 'paymentMethod': {
        //     const { billingAddress } = props;

        //     return (
        //         <PaymentsForm
        //             cancel={handleCancel}
        //             countries={countries}
        //             initialValues={billingAddress}
        //             submit={handleSubmitPaymentsForm}
        //             submitting={submitting}
        //         />
        //     );
        // }
        // case 'shippingMethod': {
        //     const { availableShippingMethods, shippingMethod } = props;
        //     return (
        //         <ShippingForm
        //             availableShippingMethods={availableShippingMethods}
        //             cancel={handleCancel}
        //             shippingMethod={shippingMethod}
        //             submit={handleSubmitShippingForm}
        //             submitting={submitting}
        //         />
        //     );
        // }
        default: {
            return null;
        }
    }
};

EditableForm.propTypes = {
    availableShippingMethods: array,
    editing: oneOf(['address', 'paymentMethod', 'shippingMethod']),
    setEditing: func.isRequired,
    shippingAddress: object,
    shippingMethod: string,
    submitting: bool,
    isAddressInvalid: bool,
    invalidAddressMessage: string
};

export default EditableForm;
