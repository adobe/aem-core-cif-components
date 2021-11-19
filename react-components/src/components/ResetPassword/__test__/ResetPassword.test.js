/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
import React from 'react';
import { render, fireEvent, waitForElement } from '@testing-library/react';
import { IntlProvider } from 'react-intl';
import { MockedProvider } from '@apollo/client/testing';

import i18nMessages from '../../../../i18n/en.json';
import MUTATION_RESET_PASSWORD from '../../../queries/mutation_reset_password.graphql';

const mockQueryParams = { get: jest.fn() };
jest.mock('../../../utils/hooks', () => ({
    useQueryParams: () => mockQueryParams
}));

import ResetPassword from '../ResetPassword';

describe('ResetPassword', () => {
    afterEach(() => {
        mockQueryParams.get.mockReset();
    });

    it('renders the form', () => {
        mockQueryParams.get.mockReturnValue('mytoken');
        const { asFragment } = render(
            <IntlProvider locale="en" messages={i18nMessages}>
                <MockedProvider>
                    <ResetPassword />
                </MockedProvider>
            </IntlProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders an error message for a missing token', () => {
        mockQueryParams.get.mockReturnValue(null);
        const { asFragment } = render(
            <IntlProvider locale="en" messages={i18nMessages}>
                <MockedProvider>
                    <ResetPassword />
                </MockedProvider>
            </IntlProvider>
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
            <IntlProvider locale="en" messages={i18nMessages}>
                <MockedProvider mocks={mocks} addTypename={false}>
                    <ResetPassword />
                </MockedProvider>
            </IntlProvider>
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
            <IntlProvider locale="en" messages={i18nMessages}>
                <MockedProvider mocks={mocks} addTypename={false}>
                    <ResetPassword />
                </MockedProvider>
            </IntlProvider>
        );

        fireEvent.change(getByLabelText('email'), { target: { value: 'chuck@example.com' } });
        fireEvent.change(getByLabelText('password'), { target: { value: 'NewPassword123' } });
        fireEvent.change(getByLabelText('confirm'), { target: { value: 'NewPassword123' } });
        fireEvent.click(getByLabelText('submit'));

        await waitForElement(() => getByText('Your password was changed. Please log in with your new password.'));
        expect(asFragment()).toMatchSnapshot();
    });
});
