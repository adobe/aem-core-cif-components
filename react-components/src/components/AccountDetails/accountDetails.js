/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import LoadingIndicator from '../LoadingIndicator';
import Button from '../Button';

import useAccountDetails from './useAccountDetails';
import GET_CUSTOMER_INFORMATION from '../../queries/query_get_customer_information.graphql';
import UPDATE_CUSTOMER_INFORMATION from '../../queries/mutation_update_customer_information.graphql';
import CHANGE_CUSTOMER_PASSWORD from '../../queries/mutation_change_password.graphql';

import classes from './accountDetails.css';
import EditModal from './editModal';

const AccountDetails = () => {
    const {
        initialValues,
        isSignedIn,
        isUpdateMode,
        showEditForm,
        handleCancel,
        shouldShowNewPasswordField,
        handleShowNewPasswordField,
        handleSubmit,
        isDisabled,
        formErrors
    } = useAccountDetails({
        getCustomerInformationQuery: GET_CUSTOMER_INFORMATION,
        setCustomerInformationMutation: UPDATE_CUSTOMER_INFORMATION,
        changeCustomerPasswordMutation: CHANGE_CUSTOMER_PASSWORD
    });
    const intl = useIntl();

    if (!isSignedIn) {
        return (
            <div className={classes.messageText}>
                {intl.formatMessage({
                    id: 'account:account-details-sign-in-text',
                    defaultMessage: 'Please sign in to see the account details.'
                })}
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
                <span className={classes.lineItemLabel}>
                    {intl.formatMessage({ id: 'account:name', defaultMessage: 'Name' })}
                </span>
                <span className={classes.lineItemValue} aria-label="name">
                    {`${customer.firstname} ${customer.lastname}`}
                </span>
                <span className={classes.lineItemLabel}>
                    {intl.formatMessage({ id: 'account:email', defaultMessage: 'Email address' })}
                </span>
                <span className={classes.lineItemValue} aria-label="email">
                    {customer.email}
                </span>
                <span className={classes.lineItemLabel}>
                    {intl.formatMessage({ id: 'account:password', defaultMessage: 'Password' })}
                </span>
                <span className={classes.lineItemValue} aria-label="password">
                    {`*******`}
                </span>
                <span className={classes.lineItemButton}>
                    <Button className={classes.editInformationButton} onClick={showEditForm}>
                        {intl.formatMessage({ id: 'common', defaultMessage: 'Edit' })}
                    </Button>
                </span>
            </div>
            <EditModal
                formErrors={formErrors}
                initialValues={{ ...customer }}
                isDisabled={isDisabled}
                isOpen={isUpdateMode}
                onCancel={handleCancel}
                onChangePassword={handleShowNewPasswordField}
                onSubmit={handleSubmit}
                shouldShowNewPassword={shouldShowNewPasswordField}
            />
        </div>
    );
};

export default AccountDetails;
