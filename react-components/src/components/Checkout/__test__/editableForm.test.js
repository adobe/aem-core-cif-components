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
import EditableForm from '../editableForm';
import { CartProvider } from '../../Minicart/cartContext';
import { CheckoutProvider } from '../checkoutContext';
import UserContextProvider from '../../../context/UserContext';

import QUERY_COUNTRIES from '../../../queries/query_countries.graphql';

describe('<EditableForm />', () => {
    it('renders the shipping address form if countries are loaded', async () => {
        const mocks = [
            {
                request: {
                    query: QUERY_COUNTRIES
                },
                result: {
                    data: {
                        countries: [
                            {
                                id: 'US',
                                available_regions: [
                                    { code: 'AL', name: 'Alabama' },
                                    { code: 'AK', name: 'Alaska' }
                                ]
                            }
                        ]
                    }
                }
            }
        ];

        const { queryByText } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{}} reducerFactory={() => state => state}>
                        <CheckoutProvider
                            initialState={{ editing: 'address', flowState: 'form' }}
                            reducer={state => state}>
                            <EditableForm />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        const result = await waitForElement(() => {
            return queryByText('Shipping Address');
        });

        expect(result).not.toBeNull();
    });

    it('does not render the shipping address form if countries could not be loaded', async () => {
        const mocks = [
            {
                request: {
                    query: QUERY_COUNTRIES
                },
                result: {
                    data: {
                        countries: []
                    }
                }
            }
        ];

        const { asFragment } = render(
            <MockedProvider mocks={mocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider initialState={{}} reducerFactory={() => state => state}>
                        <CheckoutProvider
                            initialState={{ editing: 'address', flowState: 'form' }}
                            reducer={state => state}>
                            <EditableForm />
                        </CheckoutProvider>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        expect(asFragment()).toMatchSnapshot();
    });
});
