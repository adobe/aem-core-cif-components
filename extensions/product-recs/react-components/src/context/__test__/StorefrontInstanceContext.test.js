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
import { wait } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing';
import { renderHook } from '@testing-library/react-hooks';

import { StorefrontInstanceContextProvider, useStorefrontInstanceContext } from '../StorefrontInstanceContext';
import QUERY_STOREFRONT_INSTANCE_CONTEXT from '../../queries/query_storefront_instance_context.graphql';
import mockMagentoStorefrontEvents from '../../__test__/mockMagentoStorefrontEvents';

describe('StorefrontInstanceContext', () => {
    const STORAGE_KEY = 'CIF_STOREFRONT_INSTANCE_CONTEXT';
    let mse;

    const resultData = {
        dataServicesStorefrontInstanceContext: {
            catalog_extension_version: '100.0.1',
            environment: 'Testing',
            environment_id: 'my-environment-id',
            store_code: 'main_website_store',
            store_id: 1,
            store_name: 'Main Website Store',
            store_url: 'https://my-store-url/',
            store_view_code: 'default',
            store_view_id: 1,
            store_view_name: 'Default Store View',
            website_code: 'base',
            website_id: 1,
            website_name: 'Main Website'
        },
        storeConfig: {
            base_currency_code: 'USD'
        }
    };

    const successMocks = [
        {
            request: {
                query: QUERY_STOREFRONT_INSTANCE_CONTEXT
            },
            result: {
                data: resultData
            }
        }
    ];

    beforeAll(() => {
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;
    });

    beforeEach(() => {
        window.magentoStorefrontEvents.mockClear();
        sessionStorage.removeItem(STORAGE_KEY);
    });

    it('retrieves the storefront instance context from GraphQL', async () => {
        const expectedContext = {
            environmentId: 'my-environment-id',
            environment: 'Testing',
            storeUrl: 'https://my-store-url/',
            websiteId: 1,
            websiteCode: 'base',
            storeId: 1,
            storeCode: 'main_website_store',
            storeViewId: 1,
            storeViewCode: 'default',
            websiteName: 'Main Website',
            storeName: 'Main Website Store',
            storeViewName: 'Default Store View',
            baseCurrencyCode: 'USD',
            storeViewCurrencyCode: 'USD',
            catalogExtensionVersion: '100.0.1'
        };

        // Render hook with context
        const wrapper = ({ children }) => (
            <MockedProvider mocks={successMocks} addTypename={false}>
                <StorefrontInstanceContextProvider>{children}</StorefrontInstanceContextProvider>
            </MockedProvider>
        );
        const { result } = renderHook(() => useStorefrontInstanceContext(), { wrapper });

        // Expect hook to provide correct context
        await wait(() => {
            expect(result.current.error).toBe(null);
            expect(result.current.context).toStrictEqual(expectedContext);
        });

        // Expect context stored in sessionStorage
        const storageContext = JSON.parse(sessionStorage.getItem(STORAGE_KEY));
        expect(storageContext).toEqual(resultData);

        // Expect storefront events
        expect(mse.context.setStorefrontInstance).toHaveBeenCalledWith(expectedContext);
    });

    it('deletes the session storage for invalid data', async () => {
        // Store some invalid data in session storage
        sessionStorage.setItem(STORAGE_KEY, 'some-invalid-data');

        // Render hook with context
        const wrapper = ({ children }) => (
            <MockedProvider mocks={successMocks} addTypename={false}>
                <StorefrontInstanceContextProvider>{children}</StorefrontInstanceContextProvider>
            </MockedProvider>
        );
        const { result } = renderHook(() => useStorefrontInstanceContext(), { wrapper });

        // Wait for hook to load context
        await wait(() => {
            expect(result.current.context).not.toBeNull();
        });

        // Expect correct context to be stored in sessionStorage
        const storageContext = JSON.parse(sessionStorage.getItem(STORAGE_KEY));
        expect(storageContext).toEqual(resultData);
    });

    it('returns an error', async () => {
        const errorMocks = [
            {
                request: {
                    query: QUERY_STOREFRONT_INSTANCE_CONTEXT
                },
                error: new Error('An error occurred')
            }
        ];

        // Render hook with context
        const wrapper = ({ children }) => (
            <MockedProvider mocks={errorMocks} addTypename={false}>
                <StorefrontInstanceContextProvider>{children}</StorefrontInstanceContextProvider>
            </MockedProvider>
        );
        const { result } = renderHook(() => useStorefrontInstanceContext(), { wrapper });

        // Expect hook to provide error
        await wait(() => {
            expect(result.current.error).not.toBeNull();
        });
    });
});
