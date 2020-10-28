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
import ChangePassword from '../changePassword';
import MUTATION_CHANGE_PASSWORD from '../../../queries/mutation_change_password.graphql';

describe('<ChangePassword />', () => {
    it('renders the change password form', () => {
        const { asFragment } = render(<ChangePassword showMyAccount={() => {}} />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the success message', async () => {
        const mocks = [
            {
                request: {
                    query: MUTATION_CHANGE_PASSWORD,
                    variables: {
                        currentPassword: 'old-password',
                        newPassword: 'NewPassword123'
                    }
                },
                result: {
                    data: {
                        changeCustomerPassword: {
                            id: 4
                        }
                    }
                }
            }
        ];

        const { getByLabelText, getByText } = render(<ChangePassword showMyAccount={() => {}} />, { mocks: mocks });

        const detailsFromValue = value => {
            return {
                target: {
                    value
                }
            };
        };
        fireEvent.change(getByLabelText('old-password'), detailsFromValue('old-password'));
        fireEvent.change(getByLabelText('password'), detailsFromValue('NewPassword123'));
        fireEvent.change(getByLabelText('confirm'), detailsFromValue('NewPassword123'));
        fireEvent.click(getByLabelText('submit'));

        const result = await waitForElement(() => {
            return getByText('Your password was changed.');
        });

        expect(result).not.toBeUndefined();
    });
});
