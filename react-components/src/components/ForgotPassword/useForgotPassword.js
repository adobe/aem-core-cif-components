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

import { useState, useCallback } from 'react';
import { useMutation } from '@apollo/client';

import MUTATION_REQUEST_PASSWORD_RESET_EMAIL from '../../queries/mutation_request_password_reset_email.graphql';

const useForgotPasswordForm = () => {
    const [state, setState] = useState({ loading: false, submitted: false, email: null });

    const [requestPasswordResetEmail] = useMutation(MUTATION_REQUEST_PASSWORD_RESET_EMAIL);

    const handleFormSubmit = useCallback(
        async ({ email }) => {
            setState({ ...state, loading: true });
            try {
                await requestPasswordResetEmail({ variables: { email } });
            } catch (err) {
                // Do not output any errors which might indicate if the user email actually exists or not
            } finally {
                setState({ ...state, loading: false, submitted: true, email });
            }
        },
        [requestPasswordResetEmail]
    );

    return [
        state,
        {
            handleFormSubmit
        }
    ];
};

export default useForgotPasswordForm;
