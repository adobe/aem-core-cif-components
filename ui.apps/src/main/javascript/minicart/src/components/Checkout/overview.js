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
import React, { Fragment, useCallback } from 'react';
import { bool, func, number, object, shape, string, array } from 'prop-types';
import { useMutation } from '@apollo/react-hooks';

import PaymentMethodSummary from './paymentMethodSummary';
import ShippingAddressSummary from './shippingAddressSummary';
import ShippingMethodSummary from './shippingMethodSummary';
import Section from './section';
import Button from '../Button';
import { Price } from '@magento/peregrine';
import MUTATION_PLACE_ORDER from '../../queries/mutation_place_order.graphql';

/**
 * The Overview component renders summaries for each section of the editable
 * form.
 */
const Overview = props => {
    const {
        cancelCheckout,
        cart,
        classes,
        hasPaymentMethod,
        hasShippingAddress,
        hasShippingMethod,
        paymentData,
        setEditing,
        shippingAddress,
        shippingMethod,
        receiveOrder
    } = props;

    const [placeOrder, { data, error }] = useMutation(MUTATION_PLACE_ORDER);

    const handleAddressFormClick = useCallback(() => {
        setEditing('address');
    }, [setEditing]);

    const handlePaymentFormClick = useCallback(() => {
        setEditing('paymentMethod');
    }, [setEditing]);

    const handleShippingFormClick = useCallback(() => {
        setEditing('shippingMethod');
    }, [setEditing]);

    const ready = hasShippingAddress && hasPaymentMethod && hasShippingMethod;

    const submitOrder = useCallback(() => {
        placeOrder({ variables: { cartId: cart.cartId } });
    }, [placeOrder]);

    if (error) {
        console.error(error);
    }

    if (data) {
        receiveOrder(data.placeOrder.order);
        setEditing('receipt');
    }

    return (
        <Fragment>
            <div className={classes.body}>
                <Section label="Ship To" onClick={handleAddressFormClick} showEditIcon={hasShippingAddress}>
                    <ShippingAddressSummary
                        classes={classes}
                        hasShippingAddress={hasShippingAddress}
                        shippingAddress={shippingAddress}
                    />
                </Section>
                <Section label="Pay With" onClick={handlePaymentFormClick} showEditIcon={hasPaymentMethod}>
                    <PaymentMethodSummary
                        classes={classes}
                        hasPaymentMethod={hasPaymentMethod}
                        paymentData={paymentData}
                    />
                </Section>
                <Section label="Use" onClick={handleShippingFormClick} showEditIcon={hasShippingMethod}>
                    <ShippingMethodSummary
                        classes={classes}
                        hasShippingMethod={hasShippingMethod}
                        shippingMethod={shippingMethod}
                    />
                </Section>
                <Section label="TOTAL">
                    <Price currencyCode={cart.prices.grand_total.currency} value={cart.prices.grand_total.value || 0} />
                    <br />
                    <span>{cart.items.length} Items</span>
                </Section>
            </div>
            <div className={classes.footer}>
                <Button onClick={cancelCheckout}>Back to Cart</Button>
                <Button priority="high" disabled={!ready} onClick={submitOrder}>
                    Confirm Order
                </Button>
            </div>
        </Fragment>
    );
};

Overview.propTypes = {
    cancelCheckout: func.isRequired,
    cart: shape({
        items: array.isRequired,
        cartId: string,
        prices: shape({
            grand_total: shape({
                currency: string,
                value: number
            })
        }).isRequired
    }).isRequired,
    classes: shape({
        body: string,
        footer: string
    }),
    hasPaymentMethod: bool,
    hasShippingAddress: bool,
    hasShippingMethod: bool,
    paymentData: object,
    setEditing: func,
    shippingAddress: object,
    shippingMethod: object,
    receiveOrder: func
};

export default Overview;
