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

import MUTATION_RESET_PASSWORD from '../../queries/mutation_reset_password.graphql';

const useResetPassword = () => {
    const [status, setStatus] = useState('new');

    const [resetPassword] = useMutation(MUTATION_RESET_PASSWORD);

    const handleFormSubmit = useCallback(
        async ({ email, token, password }) => {
            setStatus('loading');
            try {
                await resetPassword({ variables: { email, resetPasswordToken: token, newPassword: password } });
                setStatus('done');
            } catch (err) {
                setStatus('error');
            }
        },
        [resetPassword]
    );

    return [status, { handleFormSubmit }];
};

export default useResetPassword;
