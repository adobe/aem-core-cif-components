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

import mutationPlaceOrder from './mocks/mutationPlaceOrder';
import mutationShippingAddress from './mocks/mutationShippingAddress';
import queryCart from './mocks/queryCart';
import queryCountries from './mocks/queryCountries';
import queryCustomerCart from './mocks/queryCustomerCart';
import queryEmptyCart from './mocks/queryEmptyCart';
import queryNewCart from './mocks/queryNewCart';

const mocks = [
    mutationPlaceOrder,
    mutationShippingAddress,
    queryCart,
    queryCountries,
    queryCustomerCart,
    queryEmptyCart,
    queryNewCart
];

const AllProviders = ({ children }) => {
    return (
        <MockedProvider mocks={mocks} addTypename={false}>
            {children}
        </MockedProvider>
    );
};

/* Wrap all the React components tested with the library in a mocked Apollo provider */
const customRender = (ui, options) => render(ui, { wrapper: AllProviders, ...options });

export * from '@testing-library/react';
export { customRender as render };
