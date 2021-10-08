/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
import React, { Fragment } from 'react';
import { shape, string } from 'prop-types';

import Price from '../Price';
import { useIntl } from 'react-intl';

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
 *
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const Overview = props => {
    const { classes } = props;
    const intl = useIntl();

    const [
        { billingAddress, shippingAddress, shippingMethod, paymentMethod, cart, inProgress },
        { editShippingAddress, editBillingInformation, placeOrder, checkoutDispatch }
    ] = useOverview();

    const ready = (cart.is_virtual && paymentMethod) || (shippingAddress && paymentMethod && shippingMethod);

    if (inProgress) {
        return <LoadingIndicator message="Placing order"></LoadingIndicator>;
    }
    const submitOrder = async () => {
        await placeOrder(cart.id);
    };

    return (
        <Fragment>
            <div className={classes.body}>
                {!cart.is_virtual && (
                    <Section
                        label={intl.formatMessage({ id: 'checkout:ship-to', defaultMessage: 'Ship To' })}
                        onClick={() => editShippingAddress(shippingAddress)}
                        showEditIcon={!!shippingAddress}>
                        <ShippingAddressSummary classes={classes} />
                    </Section>
                )}
                <Section
                    label={intl.formatMessage({ id: 'checkout:pay-with', defaultMessage: 'Pay With' })}
                    onClick={() => editBillingInformation(billingAddress)}
                    showEditIcon={!!paymentMethod}
                    disabled={!cart.is_virtual && !shippingAddress}>
                    <PaymentMethodSummary classes={classes} />
                </Section>
                {!cart.is_virtual && (
                    <Section
                        label={intl.formatMessage({ id: 'checkout:use', defaultMessage: 'Use' })}
                        onClick={() => checkoutDispatch({ type: 'setEditing', editing: 'shippingMethod' })}
                        showEditIcon={!!shippingMethod}
                        disabled={!shippingAddress}>
                        <ShippingMethodSummary classes={classes} />
                    </Section>
                )}
                <Section label={intl.formatMessage({ id: 'checkout:total', defaultMessage: 'TOTAL' })}>
                    <Price currencyCode={cart.prices.grand_total.currency} value={cart.prices.grand_total.value || 0} />
                    <br />
                    <span>{cart.items.length} Items</span>
                </Section>
            </div>
            <div className={classes.footer}>
                <Button onClick={() => checkoutDispatch({ type: 'cancelCheckout' })}>
                    {' '}
                    {intl.formatMessage({ id: 'checkout:back-to-cart', defaultMessage: 'Back to cart' })}
                </Button>

                <Button priority="high" disabled={!ready} onClick={submitOrder}>
                    {intl.formatMessage({ id: 'checkout:confirm-order', defaultMessage: 'Confirm Order' })}
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
