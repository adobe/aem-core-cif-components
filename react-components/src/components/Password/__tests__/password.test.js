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
import { render } from '../../../utils/test-utils';
import Password from '../password';
import usePassword from '../usePassword';

jest.mock('../usePassword.js');

describe('<Password>', () => {
    it('should render properly', () => {
        usePassword.mockReturnValue({
            visible: true,
            togglePasswordVisibility: jest.fn()
        });
        const { asFragment } = render(
            <Password label="Password" fieldName="password" isToggleButtonHidden={true} autoComplete="password" />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('should render toggle button if isToggleButtonHidden is false', () => {
        usePassword.mockReturnValue({
            visible: false,
            togglePasswordVisibility: jest.fn()
        });
        const { asFragment } = render(
            <Password label="Password" fieldName="password" isToggleButtonHidden={false} autoComplete="password" />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('should render show button if visible is false', () => {
        usePassword.mockReturnValue({
            visible: true,
            togglePasswordVisibility: jest.fn()
        });
        const { asFragment } = render(
            <Password label="Password" fieldName="password" isToggleButtonHidden={false} autoComplete="password" />
        );
        expect(asFragment()).toMatchSnapshot();
    });
});
