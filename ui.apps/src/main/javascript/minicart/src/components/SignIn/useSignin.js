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

import { useRef, useCallback } from 'react';
import { useMutation } from '@apollo/react-hooks';

import MUTATION_GENERATE_TOKEN from '../../queries/mutation_generate_token.graphql';

export const useSignin = () => {
    const formRef = useRef(null);
    let errorMessage = '';
    const [generateCustomerToken, { data, error }] = useMutation(MUTATION_GENERATE_TOKEN);

    if (data) {
        //switch to details view
    }

    if (error) {
        //show error message
        errorMessage = error;
        console.error(error);
    }
    const handleSubmit = useCallback(
        ({ email, password }) => {
            generateCustomerToken({ variables: { email, password } });
        },
        [generateCustomerToken]
    );

    return {
        formRef,
        handleSubmit,
        errorMessage
    };
};
