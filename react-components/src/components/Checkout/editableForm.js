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
import { bool, string } from 'prop-types';
import { useMutation } from '@apollo/react-hooks';

import { useCountries } from '../../utils/hooks';

import AddressForm from './addressForm';
import PaymentsForm from './paymentsForm';
import ShippingForm from './shippingForm';
import { useCartState } from '../Minicart/cartContext';

import MUTATION_SET_SHIPPING_ADDRESS from '../../queries/mutation_save_shipping_address.graphql';
import MUTATION_SET_PAYMENT_METHOD from '../../queries/mutation_set_payment_method.graphql';
import MUTATION_SET_BILLING_ADDRESS from '../../queries/mutation_set_billing_address.graphql';
import MUTATION_SET_SHIPPING_METHOD from '../../queries/mutation_set_shipping_method.graphql';
import MUTATION_SET_EMAIL from '../../queries/mutation_set_email_on_cart.graphql';
import { useCheckoutState } from './checkoutContext';
/**
 * The EditableForm component renders the actual edit forms for the sections
 * within the form.
 */

const EditableForm = props => {
    const { submitShippingMethod, submitting, isAddressInvalid, invalidAddressMessage } = props;
    const [{ cart, cartId }, cartDispatch] = useCartState();
    const [{ editing, shippingAddress, shippingMethod, paymentMethod, billingAddress }, dispatch] = useCheckoutState();
    const countries = useCountries();

    const [setShippingAddressesOnCart, { data, error }] = useMutation(MUTATION_SET_SHIPPING_ADDRESS);

    const [setPaymentMethodOnCart, { data: paymentResult, error: paymentError }] = useMutation(
        MUTATION_SET_PAYMENT_METHOD
    );

    const [setBillingAddressOnCart, { data: billingAddressResult, error: billingAddressError }] = useMutation(
        MUTATION_SET_BILLING_ADDRESS
    );

    const [setShippingMethodsOnCart, { data: shippingMethodsResult, error: shippingMethodsError }] = useMutation(
        MUTATION_SET_SHIPPING_METHOD
    );

    const [setGuestEmailOnCart, { data: guestEmailResult, error: guestEmailError }] = useMutation(MUTATION_SET_EMAIL);

    if (error || paymentError || billingAddressError || shippingMethodsError || guestEmailError) {
        let errorObj = error || paymentError || billingAddressError || shippingMethodsError || guestEmailError;
        cartDispatch({ type: 'error', error: errorObj.toString() });
    }

    const handleCancel = useCallback(() => {
        dispatch({ type: 'endEditing' });
    }, [dispatch]);

    const handleSubmitAddressForm = useCallback(
        formValues => {
            setShippingAddressesOnCart({ variables: { cartId: cartId, countryCode: 'US', ...formValues } });
            setGuestEmailOnCart({ variables: { cartId: cartId, email: formValues.email } });
        },
        [dispatch, setShippingAddressesOnCart]
    );

    const handleSubmitPaymentsForm = useCallback(
        args => {
            if (args.billingAddress.sameAsShippingAddress) {
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
                        countryCode: 'US',
                        region: args.billingAddress.region_code
                    }
                });
            }

            setPaymentMethodOnCart({ variables: { cartId: cartId, paymentMethodCode: args.paymentMethod.code } });
        },
        [dispatch]
    );

    const handleSubmitShippingForm = useCallback(
        formValues => {
            setShippingMethodsOnCart({ variables: { cartId: cartId, ...formValues.shippingMethod } });
        },
        [dispatch, submitShippingMethod]
    );

    if (data && guestEmailResult) {
        const newShippingAddress = data.setShippingAddressesOnCart.cart.shipping_addresses[0];
        dispatch({
            type: 'setShippingAddress',
            shippingAddress: {
                ...newShippingAddress,
                email: guestEmailResult.setGuestEmailOnCart.cart.email,
                country: newShippingAddress.country.code,
                region_code: newShippingAddress.region.code
            }
        });
    }

    if (paymentResult && billingAddressResult) {
        dispatch({
            type: 'setPaymentMethod',
            paymentMethod: {
                ...paymentResult.setPaymentMethodOnCart.cart.selected_payment_method
            }
        });
        dispatch({
            type: 'setBillingAddress',
            billingAddress: {
                ...billingAddressResult.setBillingAddressOnCart.cart.billing_address
            }
        });
    }

    if (shippingMethodsResult) {
        dispatch({
            type: 'setShippingMethod',
            shippingMethod:
                shippingMethodsResult.setShippingMethodsOnCart.cart.shipping_addresses[0].selected_shipping_method
        });
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
            return (
                <PaymentsForm
                    cart={cart}
                    cancel={handleCancel}
                    countries={countries}
                    initialValues={billingAddress}
                    submit={handleSubmitPaymentsForm}
                    submitting={submitting}
                    paymentMethods={cart.available_payment_methods}
                    initialPaymentMethod={paymentMethod}
                />
            );
        }
        case 'shippingMethod': {
            const availableShippingMethods =
                shippingAddress && shippingAddress.available_shipping_methods
                    ? shippingAddress.available_shipping_methods
                    : [];
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
    submitting: bool,
    isAddressInvalid: bool,
    invalidAddressMessage: string
};

export default EditableForm;
