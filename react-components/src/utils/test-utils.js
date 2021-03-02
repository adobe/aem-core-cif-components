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
import { render } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing';
import { I18nextProvider } from 'react-i18next';
import { MockLink } from '@apollo/client/testing';
import { onError } from '@apollo/client/link/error';
import { ApolloLink, from } from '@apollo/client';

import i18n from '../../__mocks__/i18nForTests';
import ConfigContextProvider from '../context/ConfigContext';
import UserContextProvider from '../context/UserContext';

import mutationChangePassword from './mocks/mutationChangePassword';
import mutationCreateCustomer from './mocks/mutationCreateCustomer';
import mutationCreateDuplicateCustomer from './mocks/mutationCreateCustomerDuplicate';
import mutationDeleteCustomerAddress from './mocks/mutationDeleteCustomerAddress';
import mutationGenerateToken from './mocks/mutationGenerateToken';
import mutationGenerateTokenWrongPassword from './mocks/mutationGenerateTokenWrongPassword';
import mutationMergeCarts from './mocks/mutationMergeCarts';
import mutationPlaceOrder from './mocks/mutationPlaceOrder';
import mutationRevokeToken from './mocks/mutationRevokeToken';
import mutationShippingAddress from './mocks/mutationShippingAddress';
import queryCart from './mocks/queryCart';
import queryCountries from './mocks/queryCountries';
import queryCustomerCart from './mocks/queryCustomerCart';
import queryCustomerDetails from './mocks/queryCustomerDetails';
import queryCustomerInformation from './mocks/queryCustomerInformation';
import queryEmptyCart from './mocks/queryEmptyCart';
import queryNewCart from './mocks/queryNewCart';
import { gql } from '@apollo/client';

const defaultMocks = [
    mutationChangePassword,
    mutationCreateCustomer,
    mutationCreateDuplicateCustomer,
    mutationDeleteCustomerAddress,
    mutationGenerateToken,
    mutationGenerateTokenWrongPassword,
    mutationMergeCarts,
    mutationPlaceOrder,
    mutationRevokeToken,
    mutationShippingAddress,
    queryCart,
    queryCountries,
    queryCustomerCart,
    queryCustomerDetails,
    queryCustomerInformation,
    queryEmptyCart,
    queryNewCart,
    {
        request: {
            query: gql`
                mutation(
                    $cartId: String!
                    $city: String!
                    $company: String
                    $country_code: String!
                    $firstname: String!
                    $lastname: String!
                    $postcode: String
                    $region_code: String
                    $save_in_address_book: Boolean
                    $street: [String]!
                    $telephone: String!
                ) {
                    setShippingAddressesOnCart(
                        input: {
                            cart_id: $cartId
                            shipping_addresses: [
                                {
                                    address: {
                                        city: $city
                                        company: $company
                                        country_code: $country_code
                                        firstname: $firstname
                                        lastname: $lastname
                                        postcode: $postcode
                                        region: $region_code
                                        save_in_address_book: $save_in_address_book
                                        street: $street
                                        telephone: $telephone
                                    }
                                }
                            ]
                        }
                    ) {
                        cart {
                            shipping_addresses {
                                available_shipping_methods {
                                    carrier_code
                                    carrier_title
                                    method_code
                                    method_title
                                }
                                city
                                company
                                country {
                                    code
                                }
                                firstname
                                lastname
                                postcode
                                region {
                                    code
                                }
                                street
                                telephone
                            }
                        }
                    }
                }
            `,
            variables: {
                cartId: '123ABC',
                country_code: 'US',
                firstname: 'Veronica',
                lastname: 'Costello',
                email: 'veronica@example.com',
                city: 'Calder',
                region_code: 'MI',
                postcode: '49628-7978',
                telephone: '(555) 229-3326',
                street: ['cart shipping address']
            }
        },
        result: {
            data: {
                cart: {
                    shipping_addresses: [
                        {
                            available_shipping_methods: [
                                {
                                    carrier_code: 'test carrier code',
                                    carrier_title: 'test carrier title',
                                    method_code: 'test method code',
                                    method_title: 'test method title'
                                }
                            ],
                            city: 'Costello',
                            company: '',
                            country: {
                                code: 'US'
                            },
                            firstname: 'Veronica',
                            lastname: 'Costello',
                            postcode: '49628-7978',
                            region: {
                                code: 'MI'
                            },
                            street: 'cart shipping address',
                            telephone: '(555) 229-3326'
                        }
                    ]
                }
            }
        }
    }
];

const defaultConfig = {
    storeView: 'default',
    graphqlEndpoint: 'none',
    graphqlMethod: 'GET'
};

// eslint-disable-next-line react/display-name
const allProviders = (config, userContext, mocks) => ({ children }) => {
    let mockLink = new MockLink(mocks);

    let loggerLink = new ApolloLink((operation, forward) => {
        console.log(
            `[GraphQL operation]: \nQuery: ${JSON.stringify(operation.query)} \nVariables: ${JSON.stringify(
                operation.variables
            )}`
        );
        return forward(operation);
    });

    let errorLoggingLink = onError(({ graphQLErrors, networkError }) => {
        if (graphQLErrors)
            graphQLErrors.map(({ message, locations, path }) =>
                console.log(`[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`)
            );

        if (networkError) console.log(`[Network error]: ${networkError}`);
    });
    let link = from([loggerLink, errorLoggingLink, mockLink]);

    return (
        <MockedProvider addTypename={false} link={link}>
            <ConfigContextProvider config={config || defaultConfig}>
                <UserContextProvider initialState={userContext}>
                    <I18nextProvider i18n={i18n}>{children}</I18nextProvider>
                </UserContextProvider>
            </ConfigContextProvider>
        </MockedProvider>
    );
};

/* Wrap all the React components tested with the library in a mocked Apollo provider */
const customRender = (ui, options = {}) => {
    const { config, userContext, mocks = defaultMocks, ...renderOptions } = options;
    return render(ui, { wrapper: allProviders(config, userContext, mocks), ...renderOptions });
};

export * from '@testing-library/react';
export { customRender as render };
