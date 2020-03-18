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
import React from 'react';
import { useTranslation, Trans } from 'react-i18next';

import classes from './receipt.css';
import Trigger from '../Trigger';
import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';

const Receipt = () => {
    const [, cartDispatch] = useCartState();
    const [{ order }, dispatch] = useCheckoutState();
    const [t] = useTranslation('checkout');

    const continueShopping = () => {
        cartDispatch({ type: 'reset' });
        dispatch({ type: 'reset' });
    };

    return (
        <div className={classes.root}>
            <div className={classes.body}>
                <h2 className={classes.header}>
                    {t('checkout:thankyou-for-purchase', 'Thank you for your purchase!')}
                </h2>
                <div className={classes.textBlock}>
                    {/* prettier-ignore */}
                    <Trans i18nKey="checkout:order-confirmation">
                        The order number is {{ orderId : order.order_id }}. You will receive an order confirmation email with order
                        status and other details.
                    </Trans>
                </div>
                <Trigger action={continueShopping}>
                    <span className={classes.continue}>{t('checkout:continue-shopping', 'Continue Shopping')}</span>
                </Trigger>
            </div>
        </div>
    );
};

export default Receipt;
