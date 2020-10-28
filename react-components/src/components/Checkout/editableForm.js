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
import { useTranslation } from 'react-i18next';
import { useMutation } from '@apollo/client';

import { useCountries, useAwaitQuery } from '../../utils/hooks';
import { getCartDetails } from '../../actions/cart';

import AddressForm from '../AddressForm';
import PaymentsForm from './paymentsForm';
import ShippingForm from './shippingForm';
import { useAddressSelect } from '../AddressForm/useAddressSelect';
import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';
import { useUserContext } from '../../context/UserContext';

import MUTATION_SET_SHIPPING_ADDRESS from '../../queries/mutation_set_shipping_address.graphql';
import MUTATION_SET_PAYMENT_METHOD from '../../queries/mutation_set_payment_method.graphql';
import MUTATION_SET_BRAINTREE_PAYMENT_METHOD from '../../queries/mutation_set_braintree_payment_method.graphql';
import MUTATION_SET_BILLING_ADDRESS from '../../queries/mutation_set_billing_address.graphql';
import MUTATION_SET_SHIPPING_METHOD from '../../queries/mutation_set_shipping_method.graphql';
import MUTATION_SET_GUEST_EMAIL_ON_CART from '../../queries/mutation_set_guest_email_on_cart.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

/**
 * The EditableForm component renders the actual edit forms for the sections
 * within the form.
 */
const EditableForm = props => {
    const { submitting, isAddressInvalid, invalidAddressMessage } = props;
    const { parseInitialAddressSelectValue, handleChangeAddressSelectInCheckout } = useAddressSelect();
    const [{ cart, cartId }, cartDispatch] = useCartState();
    const [
        {
            editing,
            shippingAddress,
            shippingMethod,
            paymentMethod,
            billingAddress,
            billingAddressSameAsShippingAddress,
            isEditingNewAddress
        },
        dispatch
    ] = useCheckoutState();
    const { error: countriesError, countries } = useCountries();
    const [{ isSignedIn, currentUser }] = useUserContext();

    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);
    const [setShippingAddressesOnCart] = useMutation(MUTATION_SET_SHIPPING_ADDRESS);

    const [setBraintreePaymentMethodOnCart] = useMutation(MUTATION_SET_BRAINTREE_PAYMENT_METHOD);
    const [setPaymentMethodOnCart] = useMutation(MUTATION_SET_PAYMENT_METHOD);
    const [setBillingAddressOnCart] = useMutation(MUTATION_SET_BILLING_ADDRESS);

    const [setShippingMethodsOnCart, { data: shippingMethodsResult, error: shippingMethodsError }] = useMutation(
        MUTATION_SET_SHIPPING_METHOD
    );
    const [setGuestEmailOnCart] = useMutation(MUTATION_SET_GUEST_EMAIL_ON_CART);

    const [t] = useTranslation(['checkout']);

    if (shippingMethodsError || countriesError) {
        let errorObj = shippingMethodsError || countriesError;
        cartDispatch({ type: 'error', error: errorObj.toString() });
    }

    const handleCancel = useCallback(() => {
        dispatch({ type: 'endEditing' });
    }, [dispatch]);

    const handleSubmitAddressForm = async formValues => {
        cartDispatch({ type: 'beginLoading' });
        try {
            const addressVariables = { variables: { cartId, country_code: 'US', ...formValues } };
            await setShippingAddressesOnCart(addressVariables);
            if (billingAddressSameAsShippingAddress) {
                addressVariables.variables.save_in_address_book = false;
                await setBillingAddressOnCart(addressVariables);
            }
            await getCartDetails({ cartDetailsQuery, dispatch: cartDispatch, cartId });

            if (!isSignedIn) {
                await setGuestEmailOnCart({ variables: { cartId, email: formValues.email } });
                dispatch({ type: 'setShippingAddressEmail', email: formValues.email });
            }
        } catch (err) {
            cartDispatch({ type: 'error', error: err.toString() });
        } finally {
            cartDispatch({ type: 'endLoading' });
        }
    };

    const handleSubmitPaymentsForm = async formValues => {
        cartDispatch({ type: 'beginLoading' });
        try {
            const billingAddressValues =
                !cart.is_virtual && formValues.billingAddress.sameAsShippingAddress && shippingAddress
                    ? { ...shippingAddress }
                    : formValues.billingAddress;
            const addressVariables = { variables: { cartId, country_code: 'US', ...billingAddressValues } };
            await setBillingAddressOnCart(addressVariables);
            await getCartDetails({ cartDetailsQuery, dispatch: cartDispatch, cartId });

            dispatch({
                type: 'setBillingAddressSameAsShippingAddress',
                same: formValues.billingAddress.sameAsShippingAddress
            });

            // If virtual and guest cart, set email with payment address, since no shipping address set
            if (cart.is_virtual && !isSignedIn) {
                await setGuestEmailOnCart({ variables: { cartId, email: formValues.billingAddress.email } });
                dispatch({ type: 'setBillingAddressEmail', email: formValues.billingAddress.email });
            }

            let paymentResult;
            switch (formValues.paymentMethod.code) {
                case 'braintree':
                case 'braintree_paypal': {
                    paymentResult = await setBraintreePaymentMethodOnCart({
                        variables: {
                            cartId: cartId,
                            paymentMethodCode: formValues.paymentMethod.code,
                            nonce: formValues.paymentNonce
                        }
                    });
                    break;
                }
                default: {
                    paymentResult = await setPaymentMethodOnCart({
                        variables: { cartId: cartId, paymentMethodCode: formValues.paymentMethod.code }
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
        } finally {
            cartDispatch({ type: 'endLoading' });
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

    const hasSavedAddresses = currentUser.addresses.length > 0;
    const newAddressItemValue = 0;
    const billingAddressSelectInitialValue = billingAddressSameAsShippingAddress
        ? newAddressItemValue
        : parseInitialAddressSelectValue(billingAddress);

    switch (editing) {
        case 'address': {
            return (
                <AddressForm
                    cancel={handleCancel}
                    countries={countries}
                    heading={t('checkout:address-form-heading', 'Shipping Address')}
                    isAddressInvalid={isAddressInvalid}
                    invalidAddressMessage={invalidAddressMessage}
                    initialAddressSelectValue={parseInitialAddressSelectValue(shippingAddress)}
                    initialValues={shippingAddress}
                    onAddressSelectValueChange={handleChangeAddressSelectInCheckout}
                    showAddressSelect={isSignedIn && hasSavedAddresses}
                    showEmailInput={!isSignedIn}
                    showSaveInAddressBookCheckbox={isSignedIn && isEditingNewAddress}
                    submit={handleSubmitAddressForm}
                    submitting={submitting}
                    submitLabel={t('checkout:address-submit', 'Use Address')}
                />
            );
        }
        case 'paymentMethod': {
            return (
                <PaymentsForm
                    allowSame={!cart.is_virtual}
                    billingAddressSameAsShippingAddress={billingAddressSameAsShippingAddress}
                    cancel={handleCancel}
                    cart={cart}
                    countries={countries}
                    initialAddressSelectValue={billingAddressSelectInitialValue}
                    initialPaymentMethod={paymentMethod}
                    initialValues={billingAddress}
                    onAddressSelectValueChange={handleChangeAddressSelectInCheckout}
                    paymentMethods={cart.available_payment_methods}
                    showAddressSelect={isSignedIn && hasSavedAddresses}
                    showEmailInput={cart.is_virtual && !isSignedIn}
                    showSaveInAddressBookCheckbox={isSignedIn && isEditingNewAddress}
                    submit={handleSubmitPaymentsForm}
                    submitting={submitting}
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
