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
import React, { useEffect, useState } from 'react';

import { useCheckoutState } from '../checkoutContext';
import { useMutation } from '@apollo/client';
import dropIn from 'braintree-web-drop-in';
import CREATE_BRAINTREE_CLIENT_TOKEN from '../../../queries/mutation_create_braintree_client_token.graphql';
import { oneOf } from 'prop-types';
import { useFieldApi } from 'informed';
import { useCartState } from '../../Minicart/cartContext';

const CONTAINER_ID = 'braintree-dropin-container';

const Braintree = props => {
    const [{ braintreeToken }, dispatch] = useCheckoutState();
    const [{ cart }, cartDispatch] = useCartState();

    const [dropinInstance, setDropinInstance] = useState(); // This state is gone after you leave the payment form
    const [paymentMethodRequestable, setPaymentMethodRequestable] = useState(false);
    const paymentNonceField = useFieldApi('payment_nonce');

    const [createBraintreeClientToken, { data: braintreeTokenData, error: braintreeTokenError }] = useMutation(
        CREATE_BRAINTREE_CLIENT_TOKEN
    );

    async function createDropinInstance() {
        try {
            // Tear down instance if it already exists, e.g. when switching between the PayPal and credit card form.
            if (dropinInstance) {
                await dropinInstance.teardown();
                setDropinInstance(false);
            }

            let paypal = false;
            if (props.accept === 'paypal') {
                const amount = {
                    amount: cart.prices.grand_total.value,
                    currency: cart.prices.grand_total.currency
                };

                paypal = {
                    flow: 'checkout',
                    ...amount
                };
            }
            let card = false;
            if (props.accept === 'card') {
                card = {
                    cardholderName: {
                        required: true
                    },
                    overrides: {
                        fields: {
                            number: {
                                maskInput: {
                                    // Only show last four digits on blur.
                                    showLastFour: true
                                }
                            }
                        }
                    }
                };
            }

            const braintreeDropIn = await dropIn.create({
                authorization: braintreeToken,
                container: `#${CONTAINER_ID}`,
                paypal,
                card
            });

            braintreeDropIn.on('paymentMethodRequestable', () => {
                setPaymentMethodRequestable(true);
            });

            braintreeDropIn.on('noPaymentMethodRequestable', () => {
                setPaymentMethodRequestable(false);
            });

            setDropinInstance(braintreeDropIn);
        } catch (error) {
            cartDispatch({ type: 'error', error: error.toString() });
        }
    }

    // Request new braintree token and initialize payment form
    useEffect(() => {
        if (!braintreeToken) {
            createBraintreeClientToken();
            return;
        }
        createDropinInstance();
    }, [braintreeToken, props.accept]);

    // Store braintree token
    useEffect(() => {
        if (braintreeTokenError) {
            cartDispatch({ type: 'error', error: braintreeTokenError.toString() });
            return;
        }
        if (braintreeTokenData && braintreeTokenData.createBraintreeClientToken) {
            dispatch({ type: 'setBraintreeToken', token: braintreeTokenData.createBraintreeClientToken });
        }
    }, [braintreeTokenData, braintreeTokenError]);

    useEffect(() => {
        if (paymentMethodRequestable) {
            dropinInstance
                .requestPaymentMethod()
                .then(paymentNonce => {
                    paymentNonceField.setValue(paymentNonce.nonce);
                })
                .catch(error => {
                    cartDispatch({ type: 'error', error: error.toString() });
                });
        }
    }, [paymentMethodRequestable]);

    return <div id={CONTAINER_ID} />;
};

Braintree.propTypes = {
    accept: oneOf(['card', 'paypal']).isRequired
};

export default Braintree;
