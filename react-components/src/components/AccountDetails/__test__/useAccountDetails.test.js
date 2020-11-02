/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import React, { useState } from 'react';
import { fireEvent, waitForElement } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import useAccountDetails from '../useAccountDetails';

const mockSetCustomerInformation = jest.fn();
const mockChangeCustomerPassword = jest.fn();

jest.mock('@apollo/client', () => {
    const { useMutation, ...rest } = jest.requireActual('@apollo/client');

    return {
        ...rest,
        useMutation: jest.fn().mockImplementation(mutation => {
            if (mutation === 'setCustomerInformationMutation') {
                return [mockSetCustomerInformation, { loading: false }];
            } else if (mutation === 'changeCustomerPasswordMutation') {
                return [mockChangeCustomerPassword, { loading: false }];
            } else {
                return useMutation(mutation);
            }
        }),
        useQuery: jest.fn().mockReturnValue({
            data: { customer: { firstname: 'Jane', lastname: 'Doe', email: 'jdoe@gmail.com' } },
            loading: false,
            error: null
        })
    };
});

describe('useAccountDetails', () => {
    const mockProps = {
        getCustomerInformationQuery: 'getCustomerInformationQuery',
        setCustomerInformationMutation: 'setCustomerInformationMutation',
        changeCustomerPasswordMutation: 'changeCustomerPasswordMutation'
    };

    const TestComponent = props => {
        const { initialValues, handleSubmit } = useAccountDetails(mockProps);
        const [done, setDone] = useState(false);
        if (!initialValues) {
            return <p>Loading...</p>;
        }

        const update = async () => {
            await handleSubmit(props);
            setDone(true);
        };
        return (
            <div>
                <p>{done && 'Done'}</p>
                <button onClick={update}>Click this now</button>
            </div>
        );
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('calls the mutation to update the customer data', async () => {
        const mockVariables = {
            firstname: 'Jenny',
            lastname: 'Cole',
            email: 'jcole@yahoo.com',
            password: '12345'
        };

        const { getByRole, queryByText } = render(<TestComponent {...mockVariables} />, {
            userContext: { isSignedIn: true }
        });
        const button = getByRole('button');

        fireEvent.click(button);
        // wait for the DOM element to show up because the function might not be executed
        // until we assert on it.
        await waitForElement(() => queryByText('Done'));
        expect(mockSetCustomerInformation).toHaveBeenCalled();
        expect(mockChangeCustomerPassword).not.toHaveBeenCalled();
        expect(mockSetCustomerInformation.mock.calls[0][0]).toEqual({
            variables: {
                ...mockVariables
            }
        });
    });

    it('calls the mutation to update the customer password', async () => {
        const mockVariables = {
            firstname: 'Jenny',
            lastname: 'Cole',
            email: 'jcole@yahoo.com',
            password: '12345',
            newPassword: '54321'
        };

        const { getByRole, queryByText } = render(<TestComponent {...mockVariables} />);
        const button = getByRole('button');
        fireEvent.click(button);

        // wait for the DOM element to show up because the function might not be executed
        // until we assert on it.
        await waitForElement(() => queryByText('Done'));
        expect(mockChangeCustomerPassword).toHaveBeenCalled();
        expect(mockChangeCustomerPassword.mock.calls[0][0]).toEqual({
            variables: {
                currentPassword: '12345',
                newPassword: '54321'
            }
        });
    });

    it('does not call the mutation if there are no changes in the fields', async () => {
        const mockVariables = {
            firstname: 'Jane',
            lastname: 'Doe',
            email: 'jdoe@gmail.com'
        };
        const { getByRole, queryByText } = render(<TestComponent {...mockVariables} />);
        const button = getByRole('button');
        fireEvent.click(button);

        // wait for the DOM element to show up because the function might not be executed
        // until we assert on it.
        await waitForElement(() => queryByText('Done'));
        expect(mockSetCustomerInformation).not.toHaveBeenCalled();
    });

    it('opens the edit form', () => {
        const Component = () => {
            const { showEditForm, isUpdateMode, handleCancel } = useAccountDetails(mockProps);

            return (
                <div>
                    <p>{isUpdateMode ? 'Form' : ''}</p>
                    <button data-testid="show" onClick={showEditForm}>
                        Click
                    </button>
                    <button data-testid="hide" onClick={handleCancel}>
                        Click
                    </button>
                </div>
            );
        };

        const { getByTestId, queryByText } = render(<Component />);

        fireEvent.click(getByTestId('show'));
        expect(queryByText('Form')).not.toBeNull();

        fireEvent.click(getByTestId('hide'));
        expect(queryByText('Form')).toBeNull();
    });

    it('shows the new password field', () => {
        const Component = () => {
            const { shouldShowNewPasswordField, handleShowNewPasswordField } = useAccountDetails(mockProps);

            return (
                <div>
                    <p>{shouldShowNewPasswordField ? 'Password' : ''}</p>
                    <button onClick={handleShowNewPasswordField}>Click me</button>
                </div>
            );
        };

        const { getByRole, getByText } = render(<Component />);
        fireEvent.click(getByRole('button'));

        expect(getByText('Password')).not.toBeUndefined();
    });
});
