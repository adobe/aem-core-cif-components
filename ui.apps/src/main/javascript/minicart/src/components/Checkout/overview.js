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
import { shape, string } from 'prop-types';
import { useMutation } from '@apollo/react-hooks';

import PaymentMethodSummary from './paymentMethodSummary';
import ShippingAddressSummary from './shippingAddressSummary';
import ShippingMethodSummary from './shippingMethodSummary';
import Section from './section';
import Button from '../Button';
import { Price } from '@magento/peregrine';
import MUTATION_PLACE_ORDER from '../../queries/mutation_place_order.graphql';
import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';

/**
 * The Overview component renders summaries for each section of the editable
 * form.
 */
const Overview = props => {
    const { classes } = props;
    const [{ cart, cartId }, cartDispatch] = useCartState();
    const [{ shippingAddress, shippingMethod, paymentMethod }, dispatch] = useCheckoutState();

    const [placeOrder, { data, error }] = useMutation(MUTATION_PLACE_ORDER);

    const ready = shippingAddress && paymentMethod && shippingMethod;

    const submitOrder = useCallback(() => {
        placeOrder({ variables: { cartId: cartId } });
    }, [placeOrder]);

    if (error) {
        cartDispatch({ type: 'error', error: error.toString() });
    }

    if (data) {
        dispatch({ type: 'placeOrder', order: data.placeOrder.order });
    }

    return (
        <Fragment>
            <div className={classes.body}>
                <Section
                    label="Ship To"
                    onClick={() => {
                        dispatch({ type: 'setEditing', editing: 'address' });
                    }}
                    showEditIcon={!!shippingAddress}>
                    <ShippingAddressSummary classes={classes} />
                </Section>
                <Section
                    label="Pay With"
                    onClick={() => {
                        dispatch({ type: 'setEditing', editing: 'paymentMethod' });
                    }}
                    showEditIcon={!!paymentMethod}>
                    <PaymentMethodSummary classes={classes} />
                </Section>
                <Section
                    label="Use"
                    onClick={() => {
                        dispatch({ type: 'setEditing', editing: 'shippingMethod' });
                    }}
                    showEditIcon={!!shippingMethod}>
                    <ShippingMethodSummary classes={classes} />
                </Section>
                <Section label="TOTAL">
                    <Price currencyCode={cart.prices.grand_total.currency} value={cart.prices.grand_total.value || 0} />
                    <br />
                    <span>{cart.items.length} Items</span>
                </Section>
            </div>
            <div className={classes.footer}>
                <Button onClick={() => dispatch({ type: 'cancelCheckout' })}>Back to Cart</Button>
                <Button priority="high" disabled={!ready} onClick={submitOrder}>
                    Confirm Order
                </Button>
            </div>
        </Fragment>
    );
};

Overview.propTypes = {
    classes: shape({
        body: string,
        footer: string
    })
};

export default Overview;
