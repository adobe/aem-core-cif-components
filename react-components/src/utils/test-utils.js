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
    queryNewCart
];

const defaultConfig = {
    storeView: 'default',
    graphqlEndpoint: 'none'
};

// eslint-disable-next-line react/display-name
const allProviders = (config, userContext, mocks) => ({ children }) => {
    return (
        <MockedProvider mocks={mocks} addTypename={false}>
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
