/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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

const { render } = require('@testing-library/react');

import PortalPlacer from '../PortalPlacer';

describe('PortalPlacer', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    const MockComponent = props => {
        return (
            <div>
                <span>MockComponent</span>
                <pre>{JSON.stringify(props, null, 4)}</pre>
            </div>
        );
    };

    it('adds a given component to each element of the given selector', () => {
        // Add targets
        [{ a: 'test', c: 456 }, { b: '' }].map(dataset => {
            const target = document.createElement('div');
            target.className = 'target';
            for (const [key, value] of Object.entries(dataset)) {
                target.dataset[key] = value;
            }
            document.body.appendChild(target);
            return target;
        });

        // Add MockComponent to each target
        render(<PortalPlacer selector=".target" component={MockComponent} />);

        expect(document.body).toMatchSnapshot();
    });

    it('renders empty for an invalid selector', () => {
        render(<PortalPlacer selector=".target" component={MockComponent} />);

        expect(document.body).toMatchSnapshot();
    });
});
