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

import { useCountries, useAwaitQuery } from '../../utils/hooks';
import { getCartDetails } from '../../actions/cart';

import AddressForm from './addressForm';
import PaymentsForm from './paymentsForm';
import ShippingForm from './shippingForm';
import { useCartState } from '../Minicart/cartContext';

import MUTATION_SET_SHIPPING_ADDRESS from '../../queries/mutation_save_shipping_address.graphql';
import MUTATION_SET_PAYMENT_METHOD from '../../queries/mutation_set_payment_method.graphql';
import MUTATION_SET_BRAINTREE_PAYMENT_METHOD from '../../queries/mutation_set_braintree_payment_method.graphql';
import MUTATION_SET_BILLING_ADDRESS from '../../queries/mutation_set_billing_address.graphql';
import MUTATION_SET_SHIPPING_METHOD from '../../queries/mutation_set_shipping_method.graphql';
import MUTATION_SET_EMAIL from '../../queries/mutation_set_email_on_cart.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import { useCheckoutState } from './checkoutContext';
import { useUserContext } from '../../context/UserContext';

/**
 * The EditableForm component renders the actual edit forms for the sections
 * within the form.
 */
const EditableForm = props => {
    const { submitShippingMethod, submitting, isAddressInvalid, invalidAddressMessage } = props;
    const [{ cart, cartId }, cartDispatch] = useCartState();
    const [{ editing, shippingAddress, shippingMethod, paymentMethod, billingAddress }, dispatch] = useCheckoutState();
    const { error: countriesError, countries } = useCountries();
    const [{ isSignedIn }] = useUserContext();
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);
    const [setShippingAddressesOnCart, { data, error }] = useMutation(MUTATION_SET_SHIPPING_ADDRESS);

    const [setBraintreePaymentMethodOnCart] = useMutation(MUTATION_SET_BRAINTREE_PAYMENT_METHOD);
    const [setPaymentMethodOnCart] = useMutation(MUTATION_SET_PAYMENT_METHOD);
    const [setBillingAddressOnCart] = useMutation(MUTATION_SET_BILLING_ADDRESS);

    const [setShippingMethodsOnCart, { data: shippingMethodsResult, error: shippingMethodsError }] = useMutation(
        MUTATION_SET_SHIPPING_METHOD
    );

    const [setGuestEmailOnCart, { data: guestEmailResult, error: guestEmailError }] = useMutation(MUTATION_SET_EMAIL);

    if (error || shippingMethodsError || guestEmailError || countriesError) {
        let errorObj = error || shippingMethodsError || guestEmailError || countriesError;
        cartDispatch({ type: 'error', error: errorObj.toString() });
    }

    const handleCancel = useCallback(() => {
        dispatch({ type: 'endEditing' });
    }, [dispatch]);

    const handleSubmitAddressForm = async formValues => {
        cartDispatch({ type: 'beginLoading' });
        try {
            await setShippingAddressesOnCart({ variables: { cartId: cartId, countryCode: 'US', ...formValues } });
            await getCartDetails({ cartDetailsQuery, dispatch: cartDispatch, cartId });
        } catch (err) {
            cartDispatch({ type: 'error', error: err.toString() });
        } finally {
            cartDispatch({ type: 'endLoading' });
        }

        if (!isSignedIn) {
            setGuestEmailOnCart({ variables: { cartId: cartId, email: formValues.email } });
        }
    };

    const handleSubmitPaymentsForm = async args => {
        try {
            let billingAddressVariables = {
                cartId: cartId,
                ...args.billingAddress,
                countryCode: 'US',
                region: args.billingAddress.region_code
            };

            if (!cart.is_virtual && args.billingAddress.sameAsShippingAddress && shippingAddress) {
                billingAddressVariables = {
                    ...billingAddressVariables,
                    ...shippingAddress,
                    countryCode: shippingAddress.country,
                    region: shippingAddress.region.code
                };
            }

            // If virtual and guest cart, set email with payment address, since no shipping address set
            if (cart.is_virtual && !isSignedIn) {
                await setGuestEmailOnCart({ variables: { cartId: cartId, email: args.billingAddress.email } });
            }

            const billingAddressResult = await setBillingAddressOnCart({ variables: billingAddressVariables });

            dispatch({
                type: 'setBillingAddress',
                billingAddress: {
                    ...billingAddressResult.data.setBillingAddressOnCart.cart.billing_address,
                    email: args.billingAddress.email
                }
            });

            let paymentResult;
            switch (args.paymentMethod.code) {
                case 'braintree':
                case 'braintree_paypal': {
                    paymentResult = await setBraintreePaymentMethodOnCart({
                        variables: {
                            cartId: cartId,
                            paymentMethodCode: args.paymentMethod.code,
                            nonce: args.paymentNonce
                        }
                    });
                    break;
                }
                default: {
                    paymentResult = await setPaymentMethodOnCart({
                        variables: { cartId: cartId, paymentMethodCode: args.paymentMethod.code }
                    });
                }
            }

            dispatch({
                type: 'setPaymentMethod',
                paymentMethod: {
                    ...paymentResult.data.setPaymentMethodOnCart.cart.selected_payment_method
                }
            });
        } catch (err) {
            cartDispatch({ type: 'error', error: err.toString() });
        }
    };

    const handleSubmitShippingForm = async formValues => {
        cartDispatch({ type: 'beginLoading' });
        try {
            await setShippingMethodsOnCart({ variables: { cartId: cartId, ...formValues.shippingMethod } });
            await getCartDetails({ cartDetailsQuery, dispatch: cartDispatch, cartId });
        } catch (err) {
            cartDispatch({ type: 'error', error: err.toString() });
        } finally {
            cartDispatch({ type: 'endLoading' });
        }
    };

    if (data && (isSignedIn || guestEmailResult)) {
        const guestEmail = guestEmailResult ? { email: guestEmailResult.setGuestEmailOnCart.cart.email } : {};
        const newShippingAddress = data.setShippingAddressesOnCart.cart.shipping_addresses[0];
        dispatch({
            type: 'setShippingAddress',
            shippingAddress: {
                ...newShippingAddress,
                ...guestEmail,
                country: newShippingAddress.country.code,
                region_code: newShippingAddress.region.code
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

    // We can display the forms only after the countries are loaded.
    if (!countries || countries.length === 0) {
        return null;
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
                    allowSame={!cart.is_virtual}
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
