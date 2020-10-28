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
import { render, fireEvent, waitForElement } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import { MockedProvider } from '@apollo/client/testing';

import MUTATION_RESET_PASSWORD from '../../../queries/mutation_reset_password.graphql';

const mockQueryParams = { get: jest.fn() };
jest.mock('../../../utils/hooks', () => ({
    useQueryParams: () => mockQueryParams
}));

import ResetPassword from '../ResetPassword';
import i18n from '../../../../__mocks__/i18nForTests';

describe('ResetPassword', () => {
    afterEach(() => {
        mockQueryParams.get.mockReset();
    });

    it('renders the form', () => {
        mockQueryParams.get.mockReturnValue('mytoken');
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <ResetPassword />
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders an error message for a missing token', () => {
        mockQueryParams.get.mockReturnValue(null);
        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <ResetPassword />
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders an error message for a failed password reset', async () => {
        mockQueryParams.get.mockReturnValue('mytoken');
        const mocks = [
            {
                request: {
                    query: MUTATION_RESET_PASSWORD,
                    variables: {
                        email: 'chuck@example.com',
                        resetPasswordToken: 'mytoken',
                        newPassword: 'NewPassword123'
                    }
                },
                result: {
                    data: { resetPassword: false },
                    errors: [{ message: 'Cannot set the customers password' }]
                }
            }
        ];

        const { asFragment, getByLabelText, getByText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider mocks={mocks} addTypename={false}>
                    <ResetPassword />
                </MockedProvider>
            </I18nextProvider>
        );

        fireEvent.change(getByLabelText('email'), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText('password'), { target: { value: 'NewPassword123' } });
        fireEvent.change(getByLabelText('confirm'), { target: { value: 'NewPassword123' } });
        fireEvent.click(getByLabelText('submit'));

        await waitForElement(() => getByText('Could not reset password.'));
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders a success message', async () => {
        mockQueryParams.get.mockReturnValue('mytoken');
        const mocks = [
            {
                request: {
                    query: MUTATION_RESET_PASSWORD,
                    variables: {
                        email: 'chuck@example.com',
                        resetPasswordToken: 'mytoken',
                        newPassword: 'NewPassword123'
                    }
                },
                result: {
                    data: { resetPassword: true }
                }
            }
        ];

        const { asFragment, getByLabelText, getByText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider mocks={mocks} addTypename={false}>
                    <ResetPassword />
                </MockedProvider>
            </I18nextProvider>
        );

        fireEvent.change(getByLabelText('email'), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText('password'), { target: { value: 'NewPassword123' } });
        fireEvent.change(getByLabelText('confirm'), { target: { value: 'NewPassword123' } });
        fireEvent.click(getByLabelText('submit'));

        await waitForElement(() => getByText('Your password was changed. Please log in with your new password.'));
        expect(asFragment()).toMatchSnapshot();
    });
});
