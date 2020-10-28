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
import { fireEvent, waitForElement } from '@testing-library/react';
import { render } from 'test-utils';
import { useUserContext } from '../../..';
import { CartProvider } from '../../Minicart';
import CreateAccount from '../createAccount';

import MUTATION_CREATE_CUSTOMER from '../../../queries/mutation_create_customer.graphql';

describe('<CreateAccount>', () => {
    beforeEach(() => {
        jest.resetModules();
    });
    it('renders the component', () => {
        const { asFragment } = render(
            <CartProvider initialState={{ cartId: null }} reducerFactory={() => state => state}>
                <CreateAccount showMyAccount={jest.fn()} showAccountCreated={jest.fn()} />
            </CartProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('submits the form properly', async () => {
        const ContextWrapper = () => {
            const [{ createAccountEmail }] = useUserContext();
            let content;
            if (createAccountEmail !== null) {
                content = <div data-testid="success">{createAccountEmail}</div>;
            } else {
                content = <CreateAccount showMyAccount={jest.fn()} showAccountCreated={jest.fn()} />;
            }

            return content;
        };

        const { getByLabelText, getByTestId, container } = render(
            <CartProvider initialState={{ cartId: 'guest123' }} reducerFactory={() => state => state}>
                <ContextWrapper />
            </CartProvider>
        );
        const detailsFromValue = value => {
            return {
                target: {
                    value
                }
            };
        };
        fireEvent.change(getByLabelText('firstname'), detailsFromValue('Iris'));
        fireEvent.change(getByLabelText('lastname'), detailsFromValue('McCoy'));
        fireEvent.change(getByLabelText('email'), detailsFromValue('imccoy@weretail.net'));
        fireEvent.change(getByLabelText('password'), detailsFromValue('Imccoy123'));
        fireEvent.change(getByLabelText('confirm'), detailsFromValue('Imccoy123'));

        fireEvent.click(getByLabelText('submit'));

        const result = await waitForElement(() => {
            return getByTestId('success');
        });

        expect(container.querySelector('.root_error')).toBe(null);
        expect(result).not.toBeUndefined();
        expect(result.textContent).toEqual('imccoy@weretail.net');
    });

    it('handles the account creation error', async () => {
        const mockPerson = {
            email: 'imccoy@weretail.net',
            firstname: 'Iris',
            lastname: 'McCoy'
        };
        const mockPassword = 'Imccoy123';
        const mocks = [
            {
                request: {
                    query: MUTATION_CREATE_CUSTOMER,
                    variables: {
                        firstname: 'Iris',
                        lastname: 'McCoy',
                        email: 'imccoy@weretail.net',
                        password: 'Imccoy123'
                    }
                },
                result: {
                    errors: [
                        {
                            message: 'A customer with the same email address already exists in an associated website.',
                            category: 'graphql-input',
                            locations: [
                                {
                                    line: 2,
                                    column: 5
                                }
                            ],
                            path: ['createCustomer']
                        }
                    ],
                    data: {
                        createCustomer: null
                    }
                }
            }
        ];

        const ContextWrapper = () => {
            const [{ createAccountError }] = useUserContext();
            let content;
            if (createAccountError) {
                content = <div data-testid="success">{createAccountError}</div>;
            } else {
                content = <CreateAccount showMyAccount={jest.fn()} showAccountCreated={jest.fn()} />;
            }

            return content;
        };

        const { getByTestId, getByLabelText } = render(
            <CartProvider initialState={{ cartId: 'guest123' }} reducerFactory={() => state => state}>
                <ContextWrapper />
            </CartProvider>,
            { mocks: mocks }
        );

        const detailsFromValue = value => {
            return {
                target: {
                    value
                }
            };
        };
        fireEvent.change(getByLabelText('firstname'), detailsFromValue(mockPerson.firstname));
        fireEvent.change(getByLabelText('lastname'), detailsFromValue(mockPerson.lastname));
        fireEvent.change(getByLabelText('email'), detailsFromValue(mockPerson.email));
        fireEvent.change(getByLabelText('password'), detailsFromValue(mockPassword));
        fireEvent.change(getByLabelText('confirm'), detailsFromValue(mockPassword));

        fireEvent.click(getByLabelText('submit'));

        const successMessage = await waitForElement(() => {
            return getByTestId('success');
        });

        expect(successMessage).not.toBeUndefined();
        expect(successMessage.textContent).toEqual(
            'A customer with the same email address already exists in an associated website.'
        );
    });
});
