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
import { useQuery } from '@apollo/react-hooks';
import { useUserContext } from '../../context/UserContext';

const useAccountDetails = props => {
    const { getCustomerInformation } = props;
    const [{ isSignedIn }] = useUserContext();

    const [isUpdateMode, setIsUpdateMode] = useState(false);
    const [showNewPassword, setShowNewPasword] = useState(false);

    const showEditForm = useCallback(() => {
        setIsUpdateMode(true);
    }, [setIsUpdateMode]);

    const handleCancel = useCallback(() => {
        setIsUpdateMode(false);
        setShowNewPasword(false);
    }, [setIsUpdateMode, setShowNewPasword]);

    const showNewPasswordFields = useCallback(() => {
        setShowNewPasword(true);
    }, [setShowNewPasword]);

    const handleChangePassword = () => {
        console.log(`Change password mutation goes here`);
    };

    const { data: accountInformationData, loading: accountInformationLoading } = useQuery(getCustomerInformation, {
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
        isSignedIn,
        isUpdateMode,
        showEditForm,
        handleCancel,
        showNewPassword,
        showNewPasswordFields
    };
};

export default useAccountDetails;
