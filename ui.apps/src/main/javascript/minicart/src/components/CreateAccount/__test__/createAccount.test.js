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

import React from 'react';
import { render, fireEvent } from '@testing-library/react';

jest.mock('../useCreateAccount');
import useCreateAccount from '../useCreateAccount';
import CreateAccount from '../createAccount';

describe('<CreateAccount>', () => {
    const detailsFromValue = value => ({
        target: {
            value
        }
    });

    beforeEach(() => {
        jest.resetModules();
    });

    it('renders the component', () => {
        useCreateAccount.mockReturnValue([
            {
                createAccountError: null,
                isSignedIn: false,
                isCreatingCustomer: false
            },
            {
                createAccount: jest.fn()
            }
        ]);

        const { asFragment } = render(<CreateAccount showMyAccount={jest.fn()} />);

        expect(asFragment()).toMatchSnapshot();
    });

    it('submits the form', () => {
        let createAccountFn = jest.fn();
        useCreateAccount.mockReturnValue([
            {
                createAccountError: null,
                isSignedIn: false,
                isCreatingCustomer: false
            },
            {
                createAccount: createAccountFn
            }
        ]);

        const { getByLabelText } = render(<CreateAccount showMyAccount={jest.fn()} />);

        const expectedArg = {
            customer: {
                email: 'imccoy@weretail.net',
                firstname: 'Iris',
                lastname: 'McCoy'
            },
            password: 'Imccoy123',
            confirm: 'Imccoy123'
        };

        fireEvent.change(getByLabelText('firstname'), detailsFromValue(expectedArg.customer.firstname));
        fireEvent.change(getByLabelText('lastname'), detailsFromValue(expectedArg.customer.lastname));
        fireEvent.change(getByLabelText('email'), detailsFromValue(expectedArg.customer.email));
        fireEvent.change(getByLabelText('password'), detailsFromValue(expectedArg.password));
        fireEvent.change(getByLabelText('confirm'), detailsFromValue(expectedArg.confirm));
        fireEvent.click(getByLabelText('submit'));

        expect(createAccountFn).toHaveBeenCalledWith(expectedArg);
    });

    it('shows my account when signed in', () => {
        let showMyAccoungFn = jest.fn();
        useCreateAccount.mockReturnValue([
            {
                createAccountError: null,
                isSignedIn: true,
                isCreatingCustomer: false
            },
            {
                createAccount: jest.fn()
            }
        ]);

        render(<CreateAccount showMyAccount={showMyAccoungFn} />);

        expect(showMyAccoungFn).toHaveBeenCalled();
    });
});
