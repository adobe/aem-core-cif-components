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
import { Form } from 'informed';
import { render } from '@testing-library/react';

import Checkbox from '../checkbox';

const field = 'a';
const label = 'b';
const id = 'c';

const props = { field, label };

describe('<Checkbox>', () => {
    it('renders the correct tree', () => {
        const { asFragment } = render(
            <Form>
                <Checkbox {...props} id={id} />
            </Form>
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('applies `props.id` to both label and input', () => {
        const { container, getByRole } = render(
            <Form>
                <Checkbox {...props} id={id} />
            </Form>
        );

        const labelInstance = container.querySelector('label');
        const checkboxInstance = getByRole('checkbox');

        expect(checkboxInstance.id).toBe(id);
        expect(labelInstance.htmlFor).toBe(id);
        expect(labelInstance.id).toBe('');
    });

    it('applies `checked` based on `initialValue`', () => {
        const { getAllByRole } = render(
            <Form>
                <Checkbox {...props} field={'a.x'} initialValue={true} />
                <Checkbox {...props} field={'a.y'} initialValue={false} />
            </Form>
        );

        const [x, y] = getAllByRole('checkbox');

        expect(x.checked).toBe(true);
        expect(y.checked).toBe(false);
    });
});
