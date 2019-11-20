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
import { useMutation } from '@apollo/react-hooks';
import dropIn from 'braintree-web-drop-in';
import CREATE_BRAINTREE_CLIENT_TOKEN from '../../../queries/mutation_create_braintree_client_token.graphql';
import { func, oneOf } from 'prop-types';
import { useFieldApi } from 'informed';
import { useCartState } from '../../Minicart/cartContext';

const CONTAINER_ID = 'braintree-dropin-container';

const Braintree = props => {
    const [{ braintreeToken }, dispatch] = useCheckoutState();
    const [{ cart }] = useCartState();
    // TODO: Store the dropin instance somewhere, so that if I open the payment button again, it is still displayed, needs to be reset after place order?

    // TODO: Store payment information in checkout context as well and display "Payment method, MasterCard ending in 44"

    // TODO: How to restore dropin instance?
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
                console.log('Teardown dropinstance');
                await dropinInstance.teardown();
                setDropinInstance(false);
            }

            const amount = {
                amount: cart.prices.grand_total.value,
                currency: cart.prices.grand_total.currency
            };

            let paypal = false;
            if (props.accept === 'paypal') {
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

            console.log('dropin initialized');

            braintreeDropIn.on('paymentMethodRequestable', function(event) {
                console.log('paymentMethodRequestable', event);
                setPaymentMethodRequestable(true);
            });

            braintreeDropIn.on('noPaymentMethodRequestable', function() {
                console.log('noPaymentMethodRequestable', event);
                setPaymentMethodRequestable(false);
            });

            setDropinInstance(braintreeDropIn);
        } catch (err) {
            console.error(`Unable to initialize Credit Card form (Braintree). \n${err}`);
        }
    }

    // Request new braintree token and initialize payment form
    useEffect(() => {
        if (!braintreeToken) {
            console.log('No braintree token available, requesting a new one.');
            createBraintreeClientToken();
            return;
        }
        console.log('Got braintree token, initialize dropin');
        createDropinInstance();
    }, [braintreeToken, props.accept]);

    // Store braintree token
    useEffect(() => {
        if (braintreeTokenError) {
            console.error(braintreeTokenError);
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
                    console.log('got payment nonce', paymentNonce);
                    paymentNonceField.setValue(paymentNonce.nonce);
                })
                .catch(e => {
                    console.error(e);
                });
        }
    }, [paymentMethodRequestable]);

    return <div id={CONTAINER_ID} />;
};

Braintree.propTypes = {
    accept: oneOf(['card', 'paypal']).isRequired,
    setIsReady: func.isRequired
};

export default Braintree;
