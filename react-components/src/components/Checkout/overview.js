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
import { Price } from '@magento/peregrine';

import PaymentMethodSummary from './paymentMethodSummary';
import ShippingAddressSummary from './shippingAddressSummary';
import ShippingMethodSummary from './shippingMethodSummary';
import LoadingIndicator from '../LoadingIndicator';
import Section from './section';
import Button from '../Button';
import useOverview from './useOverview';

/**
 * The Overview component renders summaries for each section of the editable
 * form.
 */
const Overview = props => {
    const { classes } = props;

    const [
        { shippingAddress, shippingMethod, paymentMethod, cart, inProgress },
        { placeOrder, checkoutDispatch }
    ] = useOverview();

    const ready = shippingAddress && paymentMethod && shippingMethod;

    if (inProgress) {
        return <LoadingIndicator message="Placing order"></LoadingIndicator>;
    }
    const submitOrder = async () => {
        await placeOrder(cart.id);
    };

    return (
        <Fragment>
            <div className={classes.body}>
                <Section
                    label="Ship To"
                    onClick={() => {
                        checkoutDispatch({ type: 'setEditing', editing: 'address' });
                    }}
                    showEditIcon={!!shippingAddress}>
                    <ShippingAddressSummary classes={classes} />
                </Section>
                <Section
                    label="Pay With"
                    onClick={() => {
                        checkoutDispatch({ type: 'setEditing', editing: 'paymentMethod' });
                    }}
                    showEditIcon={!!paymentMethod}
                    disabled={!shippingAddress}>
                    <PaymentMethodSummary classes={classes} />
                </Section>
                <Section
                    label="Use"
                    onClick={() => {
                        checkoutDispatch({ type: 'setEditing', editing: 'shippingMethod' });
                    }}
                    showEditIcon={!!shippingMethod}
                    disabled={!shippingAddress}>
                    <ShippingMethodSummary classes={classes} />
                </Section>
                <Section label="TOTAL">
                    <Price currencyCode={cart.prices.grand_total.currency} value={cart.prices.grand_total.value || 0} />
                    <br />
                    <span>{cart.items.length} Items</span>
                </Section>
            </div>
            <div className={classes.footer}>
                <Button onClick={() => checkoutDispatch({ type: 'cancelCheckout' })}>Back to Cart</Button>
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
