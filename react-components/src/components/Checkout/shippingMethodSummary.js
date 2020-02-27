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
import { useCheckoutState } from './checkoutContext';
import { useTranslation } from 'react-i18next';

const ShippingMethodSummary = props => {
    const { classes } = props;
    const [{ shippingMethod }] = useCheckoutState();
    const [t] = useTranslation('checkout');

    if (!shippingMethod) {
        return (
            <span className={classes.informationPrompt}>
                {t('checkout:specify-shipping-method', 'Specify Shipping Method')}
            </span>
        );
    }

    return (
        <Fragment>
            <strong>{shippingMethod.carrier_title}</strong>
        </Fragment>
    );
};

ShippingMethodSummary.propTypes = {
    classes: shape({
        informationPrompt: string
    })
};

export default ShippingMethodSummary;
