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
import { useMutation } from '@apollo/client';
import { useUserContext } from '../../context/UserContext';

import MUTATION_CREATE_CUSTOMER from '../../queries/mutation_create_customer.graphql';

export default props => {
    const { showAccountCreated } = props;
    const [{ isSignedIn, createAccountError, inProgress }, { dispatch }] = useUserContext();

    const [createCustomer] = useMutation(MUTATION_CREATE_CUSTOMER);

    const handleCreateAccount = async formValues => {
        dispatch({ type: 'setInProgress' });
        const {
            customer: { email, firstname, lastname },
            password
        } = formValues;
        try {
            await createCustomer({
                variables: { email, password, firstname, lastname }
            });

            dispatch({ type: 'postCreateAccount', accountEmail: email });
            showAccountCreated();
        } catch (error) {
            dispatch({ type: 'createAccountError', error });
        }
    };

    return [{ isSignedIn, createAccountError, inProgress }, { createAccount: handleCreateAccount }];
};
