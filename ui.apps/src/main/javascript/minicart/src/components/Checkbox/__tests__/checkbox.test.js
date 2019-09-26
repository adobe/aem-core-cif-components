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
import { createTestInstance } from '@magento/peregrine';

import Checkbox from '../checkbox';

const field = 'a';
const label = 'b';
const classes = ['icon', 'input', 'label', 'message', 'root'].reduce((acc, key) => ({ ...acc, [key]: key }), {});

const props = { classes, field, label };

describe('<Checkbox>', () => {
    it('renders the correct tree', () => {
        const tree = createTestInstance(
            <Form>
                <Checkbox {...props} />
            </Form>
        ).toJSON();

        expect(tree).toMatchSnapshot();
    });

    it('applies `props.id` to both label and input', () => {
        const id = 'c';

        const { root } = createTestInstance(
            <Form>
                <Checkbox {...props} id={id} />
            </Form>
        );

        const labelInstance = root.findByType('label');
        const checkboxInstance = root.findByProps({ className: 'input' });

        expect(checkboxInstance.props.id).toBe(id);
        expect(labelInstance.props.htmlFor).toBe(id);
        expect(labelInstance.props.id).toBeUndefined();
    });

    it('applies `checked` based on `initialValue`', () => {
        const { root } = createTestInstance(
            <Form>
                <Checkbox {...props} field={'a.x'} initialValue={true} />
                <Checkbox {...props} field={'a.y'} initialValue={false} />
            </Form>
        );

        const [x, y] = root.findAllByType('input');

        expect(x.props.checked).toBe(true);
        expect(y.props.checked).toBe(false);
    });
});
