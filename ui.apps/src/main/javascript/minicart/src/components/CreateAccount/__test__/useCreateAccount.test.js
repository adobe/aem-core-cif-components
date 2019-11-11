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

jest.mock('../../../context/UserContext');
import { useUserContext } from '../../../context/UserContext';
import useCreateAccount from '../useCreateAccount';

describe('useCreateAccount', () => {
    afterEach(() => {
        jest.resetModules();
    });

    it('creates an account', () => {
        let createAccountFn = jest.fn();
        useUserContext.mockReturnValue([
            {
                createAccountError: null,
                isSignedIn: false,
                isCreatingCustomer: false
            },
            {
                createAccount: createAccountFn
            }
        ]);
        const [, { createAccount }] = useCreateAccount();

        createAccount({
            customer: {
                email: 'imccoy@weretail.net',
                firstname: 'Iris',
                lastname: 'McCoy'
            },
            password: 'Imccoy123',
            confirm: 'Imccoy123'
        });

        expect(createAccountFn).toHaveBeenCalled();
        const arg = createAccountFn.mock.calls[0][0];
        expect(arg).toHaveProperty('email');
        expect(arg).toHaveProperty('firstname');
        expect(arg).toHaveProperty('lastname');
        expect(arg).toHaveProperty('password');
    });
});
