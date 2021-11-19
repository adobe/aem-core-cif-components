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
import { useIntl } from 'react-intl';

import { useCheckoutState } from './checkoutContext';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const PaymentMethodSummary = props => {
    const { classes } = props;
    const [{ paymentMethod }] = useCheckoutState();
    const intl = useIntl();

    if (!paymentMethod) {
        return (
            <span className={classes.informationPrompt}>
                {intl.formatMessage({
                    id: 'checkout:add-billing-information',
                    defaultMessage: 'Add Billing Information'
                })}
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
