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
import { I18nextProvider } from 'react-i18next';

import ForgotPassword from '../forgotPassword';
import UserContextProvider from '../../../context/UserContext';
import { MockedProvider } from '@apollo/react-testing';
import i18n from '../../../../__mocks__/i18nForTests';

describe('ForgotPassword', () => {
    it('renders the "forgot password" form ', () => {
        const Wrapper = () => {
            return <ForgotPassword onClose={jest.fn()} />;
        };

        const { asFragment } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider>
                        <Wrapper />
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the success message after the "forgot password" form is submitted', () => {
        const Wrapper = () => {
            return <ForgotPassword onClose={jest.fn()} />;
        };

        const { getByLabelText } = render(
            <I18nextProvider i18n={i18n}>
                <MockedProvider>
                    <UserContextProvider>
                        <Wrapper />
                    </UserContextProvider>
                </MockedProvider>
            </I18nextProvider>
        );

        fireEvent.change(getByLabelText('email'), { target: { value: 'chuck@example.com' } });
        fireEvent.click(getByLabelText('submit'));

        expect(getByLabelText('continue-shopping')).not.toBeUndefined();
    });
});
