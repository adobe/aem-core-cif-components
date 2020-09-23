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
import { useQuery, useMutation } from '@apollo/react-hooks';
import { useUserContext } from '../../context/UserContext';

const useAccountDetails = props => {
    const { getCustomerInformationQuery, setCustomerInformationMutation } = props;
    const [{ isSignedIn }] = useUserContext();

    const [isUpdateMode, setIsUpdateMode] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);

    const [
        setCustomerInformation,
        { data: updateCustomerInformationData, loading: isUpdatingCustomer, error: customerInformationUpdateError }
    ] = useMutation(setCustomerInformationMutation);

    const showEditForm = useCallback(() => {
        setIsUpdateMode(true);
    }, [setIsUpdateMode]);

    const handleCancel = useCallback(() => {
        setIsUpdateMode(false);
        setShowNewPassword(false);
    }, [setIsUpdateMode, setShowNewPassword]);

    const showNewPasswordFields = useCallback(() => {
        setShowNewPassword(true);
    }, [setShowNewPassword]);

    const handleSubmit = async values => {
        console.log(`Got some values`, values);
        const { firstname, lastname, email, password } = values;
        try {
            if (
                initialValues.firstname !== firstname ||
                initialValues.lastname !== lastname ||
                initialValues.email !== email
            ) {
                await setCustomerInformation({
                    variables: {
                        firstname,
                        lastname,
                        email,
                        password
                    }
                });

                handleCancel();
            }
        } catch {
            return;
        }
    };

    const { data: accountInformationData, loading: accountInformationLoading } = useQuery(getCustomerInformationQuery, {
        skip: !isSignedIn,
        fetchPolicy: 'cache-and-network',
        nextFetchPolicy: 'cache-first'
    });

    const initialValues = useMemo(() => {
        if (accountInformationData) {
            return {
                customer: accountInformationData.customer
            };
        }
    }, [accountInformationData]);

    return {
        initialValues,
        formErrors: [customerInformationUpdateError],
        isSignedIn,
        isUpdateMode,
        isDisabled: isUpdatingCustomer,
        showEditForm,
        handleCancel,
        handleSubmit,
        showNewPassword,
        showNewPasswordFields
    };
};

export default useAccountDetails;
