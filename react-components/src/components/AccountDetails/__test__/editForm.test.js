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

import React from 'react';
import { render } from '../../../utils/test-utils';
import EditForm from '../editForm';

jest.mock('informed', () => {
    const informed = jest.requireActual('informed');

    return {
        ...informed,
        useFormApi: () => {
            return {
                setValue: () => {}
            };
        }
    };
});

describe('<EditForm>', () => {
    afterAll(() => {
        jest.clearAllMocks();
    });

    it('renders a form with the "Change password" button', () => {
        const { asFragment } = render(<EditForm />);
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders a form without the "Change password" button', () => {
        const { asFragment } = render(<EditForm shouldShowNewPassword={true} />);
        expect(asFragment()).toMatchSnapshot();
    });
});
