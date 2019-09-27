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
import { array, bool, func, object, oneOf, string } from 'prop-types';
import { useMutation } from '@apollo/react-hooks';

import { useCountries } from '../../utils/hooks';

import AddressForm from './addressForm';
import PaymentsForm from './paymentsForm';
import ShippingForm from './shippingForm';
import { useCartState } from '../../utils/state';

import MUTATION_SET_SHIPPING_ADDRESS from '../../queries/mutation_save_shipping_address.graphql';
import MUTATION_SET_PAYMENT_METHOD from '../../queries/mutation_set_payment_method.graphql';
import MUTATION_SET_BILLING_ADDRESS from '../../queries/mutation_set_billing_address.graphql';
import MUTATION_SET_SHIPPING_METHOD from '../../queries/mutation_set_shipping_method.graphql';
import MUTATION_SET_EMAIL from '../../queries/mutation_set_email_on_cart.graphql';
/**
 * The EditableForm component renders the actual edit forms for the sections
 * within the form.
 */

const EditableForm = props => {
    const {
        editing,
        setEditing,
        submitShippingMethod,
        submitting,
        setShippingAddress,
        setBillingAddress,
        setPaymentData,
        isAddressInvalid,
        invalidAddressMessage,
        initialPaymentMethod,
        shippingAddress,
        setShippingMethod,
        shippingMethod,
        availableShippingMethods
    } = props;
    const [{ cart, cartId }] = useCartState();

    let countries = useCountries();

    const [setShippingAddressesOnCart, { data }] = useMutation(MUTATION_SET_SHIPPING_ADDRESS);

    const [setPaymentMethodOnCart, { data: paymentResult }] = useMutation(MUTATION_SET_PAYMENT_METHOD);

    const [setBillingAddressOnCart, { data: billingAddressResult }] = useMutation(MUTATION_SET_BILLING_ADDRESS);

    const [setShippingMethodsOnCart, { data: shippingMethodsResult }] = useMutation(MUTATION_SET_SHIPPING_METHOD);

    const [setGuestEmailOnCart, { data: guestEmailResult }] = useMutation(MUTATION_SET_EMAIL);

    const handleCancel = useCallback(() => {
        setEditing(null);
    }, [setEditing]);

    const handleSubmitAddressForm = useCallback(
        formValues => {
            setShippingAddressesOnCart({ variables: { cartId: cartId, countryCode: 'US', ...formValues } });
            setGuestEmailOnCart({ variables: { cartId: cartId, email: formValues.email } });
        },
        [setEditing, setShippingAddressesOnCart]
    );

    const handleSubmitPaymentsForm = useCallback(
        args => {
            if (args.billingAddress.sameAsShippingAddress) {
                const { shippingAddress } = props;
                if (shippingAddress) {
                    setBillingAddressOnCart({
                        variables: {
                            cartId: cartId,
                            ...shippingAddress,
                            countryCode: shippingAddress.country,
                            region: shippingAddress.region.code
                        }
                    });
                }
            } else {
                setBillingAddressOnCart({
                    variables: {
                        cartId: cartId,
                        ...args.billingAddress,
                        countryCode: 'US'
                    }
                });
            }

            setPaymentMethodOnCart({ variables: { cartId: cartId, paymentMethodCode: args.paymentMethod.code } });
        },
        [setEditing]
    );

    const handleSubmitShippingForm = useCallback(
        formValues => {
            setShippingMethodsOnCart({ variables: { cartId: cartId, ...formValues.shippingMethod } });
        },
        [setEditing, submitShippingMethod]
    );

    if (data && guestEmailResult) {
        const shippingAddress = data.setShippingAddressesOnCart.cart.shipping_addresses[0];
        setShippingAddress({
            ...shippingAddress,
            email: guestEmailResult.setGuestEmailOnCart.cart.email,
            country: shippingAddress.country.code,
            region_code: shippingAddress.region.code
        });
        setEditing(null);
    }

    if (paymentResult && billingAddressResult) {
        setPaymentData({
            ...paymentResult.setPaymentMethodOnCart.cart.selected_payment_method
        });
        setBillingAddress({ ...billingAddressResult.setBillingAddressOnCart.cart.billing_address });
        setEditing(null);
    }

    if (shippingMethodsResult) {
        setShippingMethod(
            shippingMethodsResult.setShippingMethodsOnCart.cart.shipping_addresses[0].selected_shipping_method
        );
        setEditing(null);
    }

    switch (editing) {
        case 'address': {
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
        case 'paymentMethod': {
            const { billingAddress } = props;

            return (
                <PaymentsForm
                    cart={cart}
                    cancel={handleCancel}
                    countries={countries}
                    initialValues={billingAddress}
                    submit={handleSubmitPaymentsForm}
                    submitting={submitting}
                    paymentMethods={cart.available_payment_methods}
                    initialPaymentMethod={initialPaymentMethod}
                />
            );
        }
        case 'shippingMethod': {
            return (
                <ShippingForm
                    availableShippingMethods={availableShippingMethods}
                    cancel={handleCancel}
                    shippingMethod={shippingMethod}
                    submit={handleSubmitShippingForm}
                    submitting={submitting}
                />
            );
        }
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
    shippingMethod: object,
    submitting: bool,
    isAddressInvalid: bool,
    invalidAddressMessage: string
};

export default EditableForm;
