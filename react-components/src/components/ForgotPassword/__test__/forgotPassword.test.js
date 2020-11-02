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
import ForgotPassword from '../forgotPassword';

import MUTATION_REQUEST_PASSWORD_RESET_EMAIL from '../../../queries/mutation_request_password_reset_email.graphql';

describe('ForgotPassword', () => {
    it('renders the "forgot password" form ', () => {
        const { asFragment } = render(<ForgotPassword onClose={jest.fn()} onCancel={jest.fn()} />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the success message after the "forgot password" form is submitted', async () => {
        const mocks = [
            {
                request: {
                    query: MUTATION_REQUEST_PASSWORD_RESET_EMAIL,
                    variables: {
                        email: 'chuck@example.com'
                    }
                },
                result: {
                    data: { requestPasswordResetEmail: true }
                }
            }
        ];

        const { getByLabelText } = render(<ForgotPassword onClose={jest.fn()} onCancel={jest.fn()} />, {
            mocks: mocks
        });

        fireEvent.change(getByLabelText('email'), { target: { value: 'chuck@example.com' } });
        fireEvent.click(getByLabelText('submit'));

        await waitForElement(() => getByLabelText('continue-shopping'));
        expect(getByLabelText('continue-shopping')).not.toBeUndefined();
    });
});
