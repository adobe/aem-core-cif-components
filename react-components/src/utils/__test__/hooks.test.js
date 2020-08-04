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
import { render, waitForElement } from '@testing-library/react';

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
                        available_regions: [
                            { id: 835, code: 'AB', name: 'Alba' },
                            { id: 838, code: 'AR', name: 'Arad' }
                        ]
                    },
                    {
                        id: 'US',
                        available_regions: [
                            { id: 4, code: 'AL', name: 'Alabama' },
                            { id: 7, code: 'AK', name: 'Alaska' }
                        ]
                    }
                ]
            }
        }
    }
];

describe('Custom hooks', () => {
    describe('useCountries', () => {
        it('returns the correct country list', async () => {
            const HookWrapper = () => {
                const { error, countries } = useCountries();
                if (error || !countries || countries.length === 0) {
                    return <div id="results"></div>;
                }
                return (
                    <div id="results">
                        <div data-testid="count">{countries.length}</div>
                        <div data-testid="result">{countries[1].id}</div>
                    </div>
                );
            };

            const { getByTestId } = render(
                <MockedProvider mocks={mocks} addTypename={false}>
                    <HookWrapper />
                </MockedProvider>
            );
            const [count, result] = await waitForElement(() => [getByTestId('count'), getByTestId('result')]);
            expect(count.textContent).toEqual('2');
            expect(result.textContent).toEqual('US');
        });
    });
});
