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
import React from 'react';
import { useIntl } from 'react-intl';

import classes from './receipt.css';
import Trigger from '../Trigger';

import useReceipt from './useReceipt';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const Receipt = () => {
    const [{ orderId }, continueShopping] = useReceipt();
    const intl = useIntl();

    return (
        <div className={classes.root}>
            <div className={classes.body}>
                <h2 className={classes.header}>
                    {intl.formatMessage({
                        id: 'checkout:thankyou-for-purchase',
                        defaultMessage: 'Thank you for your purchase!'
                    })}
                </h2>
                <div className={classes.textBlock}>
                    {intl.formatMessage(
                        {
                            id: 'checkout:order-confirmation',
                            defaultMessage:
                                'The order number is {orderId}. You will receive an order confirmation email with order status and other details.'
                        },
                        { orderId }
                    )}
                </div>
                <Trigger action={continueShopping}>
                    <span className={classes.continue}>
                        {intl.formatMessage({ id: 'checkout:continue-shopping', defaultMessage: 'Continue Shopping' })}
                    </span>
                </Trigger>
            </div>
        </div>
    );
};

export default Receipt;
