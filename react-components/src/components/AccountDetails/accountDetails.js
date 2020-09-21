/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import { useTranslation } from 'react-i18next';
import LoadingIndicator from '../LoadingIndicator';
import Button from '../Button';

import useAccountDetails from './useAccountDetails';
import GET_CUSTOMER_INFORMATION from './query_get_customer_information.graphql';
import classes from './accountDetails.css';

const AccountDetails = () => {
    const [t] = useTranslation('account');
    const { initialValues, isSignedIn } = useAccountDetails({ getCustomerInformation: GET_CUSTOMER_INFORMATION });

    if (!isSignedIn) {
        return (
            <div className={classes.messageText}>
                {t('account:address-book-sign-in-text', 'Please Sign in to see the account details.')}
            </div>
        );
    }

    if (!initialValues) {
        return <LoadingIndicator />;
    }
    const { customer } = initialValues;

    return (
        <div className={classes.accountDetails}>
            <div className={classes.lineItems}>
                <span className={classes.lineItemLabel}>{t('account', 'Name')}</span>
                <span className={classes.lineItemValue} aria-label="name">
                    {`${customer.firstname} ${customer.lastname}`}
                </span>
                <span className={classes.lineItemLabel}>{t('account', 'Email')}</span>
                <span className={classes.lineItemValue} aria-label="email">
                    {customer.email}
                </span>
                <span className={classes.lineItemLabel}>{t('account', 'Passsword')}</span>
                <span className={classes.lineItemValue} aria-label="password">
                    {`*******`}
                </span>
                <span className={classes.lineItemButton}>
                    <Button className={classes.editInformationButton}>{t('common', 'Edit')}</Button>
                </span>
            </div>
        </div>
    );
};

export default AccountDetails;
