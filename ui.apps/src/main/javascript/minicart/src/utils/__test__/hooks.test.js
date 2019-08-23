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
import { MockedProvider } from '@apollo/react-testing';
import { render, unmountComponentAtNode } from 'react-dom';
import { act } from 'react-dom/test-utils';
import { useCountries } from '../hooks';
import QUERY_COUNTRIES from '../../queries/query_countries.graphql';

const mocks = [
    {
        request: {
            query: QUERY_COUNTRIES
        },
        result: {
            data: {
                countries: [
                    {
                        id: 'RO',
                        available_regions: [{ code: 'AB', name: 'Alba' }, { code: 'AR', name: 'Arad' }]
                    },
                    {
                        id: 'US',
                        available_regions: [{ code: 'AL', name: 'Alabama' }, { code: 'AK', name: 'Alaska' }]
                    }
                ]
            }
        }
    }
];

let container = null;

describe('Custom hooks', () => {
    describe('useCountries', () => {
        beforeEach(() => {
            // setup a DOM element as a render target
            container = document.createElement('div');
            document.body.appendChild(container);
        });

        afterEach(() => {
            // cleanup on exiting
            unmountComponentAtNode(container);
            container.remove();
            container = null;
        });

        it('works', async () => {
            let results;
            const HookWrapper = () => {
                results = useCountries();
                if (!results || results.length === 0) {
                    return <div id="results"></div>;
                }
                return (
                    <div id="results">
                        <div className="count">{results.length}</div>
                        <div className="content">{results[1].id}</div>
                    </div>
                );
            };
            await act(async () => {
                render(
                    <MockedProvider mocks={mocks} addTypename={false}>
                        <HookWrapper />
                    </MockedProvider>,
                    container,
                    () => {
                        console.log('Rendered!');
                    }
                );
            });

            expect(container.querySelector('#results .count').textContent).toEqual('2');
            expect(container.querySelector('#results .content').textContent).toEqual('US');
        });
    });
});
