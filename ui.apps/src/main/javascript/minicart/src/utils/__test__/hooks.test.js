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

import { useCountries, useGuestCart } from '../hooks';
import QUERY_COUNTRIES from '../../queries/query_countries.graphql';
import MUTATION_CREATE_CART from '../../queries/mutation_create_guest_cart.graphql';

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

const GuestCartHookWrapper = () => {
    let [cartId, resetCart] = useGuestCart();
    if (!cartId || cartId.length === 0) {
        return <div data-testid="cart-details">No cart</div>;
    }
    return (
        <div>
            <div data-testid="cart-details">{cartId}</div>
            <button onClick={resetCart}>Reset</button>
        </div>
    );
};

describe('Custom hooks', () => {
    describe('useCountries', () => {
        it('returns the correct country list', async () => {
            const HookWrapper = () => {
                let results = useCountries();
                if (!results || results.length === 0) {
                    return <div id="results"></div>;
                }
                return (
                    <div id="results">
                        <div data-testid="count">{results.length}</div>
                        <div data-testid="result">{results[1].id}</div>
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

    describe('useGuestCart', () => {
        it('retrieves the id of the cart from the cookie', async () => {
            Object.defineProperty(window.document, 'cookie', {
                writable: true,
                value: 'cif.cart=cart-from-cookie;path=/;domain=http://localhost;Max-Age=3600'
            });

            const { getByTestId } = render(
                <MockedProvider mocks={[]} addTypename={false}>
                    <GuestCartHookWrapper />
                </MockedProvider>
            );
            const cartIdNode = await waitForElement(() => getByTestId('cart-details'));

            expect(cartIdNode.textContent).toEqual('cart-from-cookie');
            Object.defineProperty(window.document, 'cookie', {
                writable: true,
                value: ''
            });
        });
        it('retrieves the id of the cart from the backend if the cookie is not set', async () => {
            const { getByTestId } = render(
                <MockedProvider
                    mocks={[
                        {
                            request: {
                                query: MUTATION_CREATE_CART
                            },
                            result: {
                                data: {
                                    createEmptyCart: 'guest123'
                                }
                            }
                        }
                    ]}
                    addTypename={false}>
                    <GuestCartHookWrapper />
                </MockedProvider>
            );
            const cartIdNode = await waitForElement(() => getByTestId('cart-details'));

            expect(cartIdNode.children[0].textContent).toEqual('guest123');
        });
    });
});
