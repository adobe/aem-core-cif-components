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
import React, { Fragment } from 'react';
import { shape, string } from 'prop-types';

import Price from '../Price';
import { useTranslation } from 'react-i18next';

import PaymentMethodSummary from './paymentMethodSummary';
import ShippingAddressSummary from './shippingAddressSummary';
import ShippingMethodSummary from './shippingMethodSummary';
import LoadingIndicator from '../LoadingIndicator';
import Section from './section';
import Button from '../Button';
import useOverview from './useOverview';
import * as dataLayerUtils from '../../utils/dataLayerUtils';

/**
 * The Overview component renders summaries for each section of the editable
 * form.
 */
const Overview = props => {
    const { classes } = props;
    const [t] = useTranslation('checkout');

    const [
        { billingAddress, shippingAddress, shippingMethod, paymentMethod, cart, inProgress },
        { editShippingAddress, editBillingInformation, placeOrder, checkoutDispatch }
    ] = useOverview();

    const ready = (cart.is_virtual && paymentMethod) || (shippingAddress && paymentMethod && shippingMethod);

    if (inProgress) {
        return <LoadingIndicator message="Placing order"></LoadingIndicator>;
    }
    const submitOrder = async () => {
        const {
            placeOrder: {
                order: { order_id }
            }
        } = await placeOrder(cart.id);
        const {
            prices: {
                grand_total: { currency, value }
            },
            selected_payment_method: { code },
            items
        } = cart;
        dataLayerUtils.pushEvent('cif:placeOrder', {
            'xdm:purchaseOrderNumber': order_id,
            'xdm:currencyCode': currency,
            'xdm:priceTotal': value,
            'xdm:payments': [
                {
                    'xdm:paymentAmount': value,
                    'xdm:paymentType': code,
                    'xdm:currencyCode': currency
                }
            ],
            'xdm:products': items.map(item => {
                const {
                    product: { sku },
                    quantity
                } = item;
                return {
                    '@id': `product-${sku}`,
                    'xdm:SKU': sku,
                    'xdm:quantity': quantity
                };
            })
        });
    };

    return (
        <Fragment>
            <div className={classes.body}>
                {!cart.is_virtual && (
                    <Section
                        label={t('checkout:ship-to', 'Ship To')}
                        onClick={() => editShippingAddress(shippingAddress)}
                        showEditIcon={!!shippingAddress}>
                        <ShippingAddressSummary classes={classes} />
                    </Section>
                )}
                <Section
                    label={t('checkout:pay-with', 'Pay With')}
                    onClick={() => editBillingInformation(billingAddress)}
                    showEditIcon={!!paymentMethod}
                    disabled={!cart.is_virtual && !shippingAddress}>
                    <PaymentMethodSummary classes={classes} />
                </Section>
                {!cart.is_virtual && (
                    <Section
                        label={t('checkout:use', 'Use')}
                        onClick={() => checkoutDispatch({ type: 'setEditing', editing: 'shippingMethod' })}
                        showEditIcon={!!shippingMethod}
                        disabled={!shippingAddress}>
                        <ShippingMethodSummary classes={classes} />
                    </Section>
                )}
                <Section label={t('checkout:total', 'TOTAL')}>
                    <Price currencyCode={cart.prices.grand_total.currency} value={cart.prices.grand_total.value || 0} />
                    <br />
                    <span>{cart.items.length} Items</span>
                </Section>
            </div>
            <div className={classes.footer}>
                <Button onClick={() => checkoutDispatch({ type: 'cancelCheckout' })}>
                    {' '}
                    {t('checkout:back-to-cart', 'Back to cart')}
                </Button>

                <Button priority="high" disabled={!ready} onClick={submitOrder}>
                    {t('checkout:confirm-order', 'Confirm Order')}
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
