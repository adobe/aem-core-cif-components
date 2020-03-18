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
import { useTranslation } from 'react-i18next';

import { useCheckoutState } from './checkoutContext';

const PaymentMethodSummary = props => {
    const { classes } = props;
    const [{ paymentMethod }] = useCheckoutState();
    const [t] = useTranslation('checkout');

    if (!paymentMethod) {
        return (
            <span className={classes.informationPrompt}>
                {t('checkout:add-billing-information', 'Add Billing Information')}
            </span>
        );
    }

    return (
        <Fragment>
            <strong className={classes.paymentDisplayPrimary}>{paymentMethod.title}</strong>
            <br />
            <span className={classes.paymentDisplaySecondary}>{paymentMethod.description}</span>
        </Fragment>
    );
};

PaymentMethodSummary.propTypes = {
    classes: shape({
        informationPrompt: string,
        paymentDisplayPrimary: string,
        paymentDisplaySecondary: string
    })
};

export default PaymentMethodSummary;
