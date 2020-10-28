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
import { useMemo, useCallback, useState } from 'react';
import { useQuery, useMutation } from '@apollo/client';
import { useUserContext } from '../../context/UserContext';

const useAccountDetails = props => {
    const { getCustomerInformationQuery, setCustomerInformationMutation, changeCustomerPasswordMutation } = props;
    const [{ isSignedIn }] = useUserContext();

    // control the whether the edit form is shown or not
    const [isUpdateMode, setIsUpdateMode] = useState(false);

    // control whether we show the "new password" field
    const [shouldShowNewPasswordField, setShowNewPassword] = useState(false);

    // control whether we show the error or not.
    const [displayError, setDisplayError] = useState(false);

    const { data: accountInformationData } = useQuery(getCustomerInformationQuery, {
        skip: !isSignedIn,
        fetchPolicy: 'cache-and-network',
        nextFetchPolicy: 'cache-first'
    });

    const [
        setCustomerInformation,
        { loading: isUpdatingCustomer, error: customerInformationUpdateError }
    ] = useMutation(setCustomerInformationMutation, { refetchQueries: ['GetCustomerInformation'] });

    const [changeCustomerPassword, { loading: isChangingCustomerPassword, error: changePasswordError }] = useMutation(
        changeCustomerPasswordMutation
    );

    const initialValues = useMemo(() => {
        if (accountInformationData) {
            return {
                customer: accountInformationData.customer
            };
        }
    }, [accountInformationData]);

    const showEditForm = useCallback(() => {
        setIsUpdateMode(true);
        // hide the previous error message, if any
        setDisplayError(false);
    }, [setIsUpdateMode]);

    const handleCancel = useCallback(() => {
        setIsUpdateMode(false);
        setShowNewPassword(false);
    }, [setIsUpdateMode, setShowNewPassword]);

    const handleShowNewPasswordField = useCallback(() => {
        setShowNewPassword(true);
    }, [setShowNewPassword]);

    const handleSubmit = useCallback(
        async values => {
            const { firstname, lastname, email, password, newPassword } = values;
            try {
                if (
                    initialValues.customer.firstname !== firstname ||
                    initialValues.customer.lastname !== lastname ||
                    initialValues.customer.email !== email
                ) {
                    // we have to send the password when we change the email address
                    await setCustomerInformation({
                        variables: {
                            firstname,
                            lastname,
                            email,
                            password
                        }
                    });
                }
                // if we have a "new password" value we must change the password as well
                if (password && newPassword) {
                    await changeCustomerPassword({
                        variables: {
                            currentPassword: password,
                            newPassword
                        }
                    });
                }
                handleCancel();
            } catch {
                setDisplayError(true);
                return;
            }
        },
        [setCustomerInformation, handleCancel, changeCustomerPassword, initialValues]
    );

    const errors = displayError ? [customerInformationUpdateError, changePasswordError] : [];

    return {
        initialValues,
        formErrors: errors,
        isSignedIn,
        isUpdateMode,
        isDisabled: isUpdatingCustomer || isChangingCustomerPassword,
        showEditForm,
        handleCancel,
        handleSubmit,
        shouldShowNewPasswordField,
        handleShowNewPasswordField
    };
};

export default useAccountDetails;
