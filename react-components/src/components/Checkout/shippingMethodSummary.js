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
import { useCheckoutState } from './checkoutContext';
import { useIntl } from 'react-intl';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const ShippingMethodSummary = props => {
    const { classes } = props;
    const [{ shippingMethod }] = useCheckoutState();
    const intl = useIntl();

    if (!shippingMethod) {
        return (
            <span className={classes.informationPrompt}>
                {intl.formatMessage({
                    id: 'checkout:specify-shipping-method',
                    defaultMessage: 'Specify Shipping Method'
                })}
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
