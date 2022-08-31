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
import { renderHook } from '@testing-library/react-hooks';
import RecommendationsClient from '@magento/recommendations-js-sdk';
import { wait } from '@testing-library/react';

import { StorefrontInstanceContext } from '../../context/StorefrontInstanceContext';
import { useRecommendations } from '../useRecommendations';
import mockMagentoStorefrontEvents from '../../__test__/mockMagentoStorefrontEvents';

// Mock RecommendationsClient
const mockFetch = jest.fn();
const mockFetchPreconfigured = jest.fn();
const mockRegister = jest.fn();
jest.mock('@magento/recommendations-js-sdk', () => {
    return jest.fn().mockImplementation(() => {
        return {
            fetch: mockFetch,
            fetchPreconfigured: mockFetchPreconfigured,
            register: mockRegister,
            _storeViewCode: 'myStoreViewCode'
        };
    });
});

describe('useRecommendations', () => {
    const wrapper = ({ children }) => (
        <StorefrontInstanceContext.Provider value={{ context: {}, error: null, mse }}>
            {children}
        </StorefrontInstanceContext.Provider>
    );

    const units = [
        {
            unitId: 'my-unit'
        }
    ];

    let mse;

    beforeAll(() => {
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;
    });

    beforeEach(() => {
        window.magentoStorefrontEvents.mockClear();
        RecommendationsClient.mockClear();
        mockFetch.mockClear();
        mockFetchPreconfigured.mockClear();
        mockRegister.mockClear();
    });

    it('returns null if there is a storefront instance context error', () => {
        // Render hook with mock StorefrontInstanceContext
        const errorWrapper = ({ children }) => (
            <StorefrontInstanceContext.Provider value={{ context: null, error: {} }}>
                {children}
            </StorefrontInstanceContext.Provider>
        );
        const { result } = renderHook(() => useRecommendations({}), { wrapper: errorWrapper });

        // Expect units to be null
        expect(result.current).toStrictEqual({ loading: false, units: null });
    });

    it('returns null if there is a recommendations SDK error', async () => {
        // Prepare mocks
        mockFetchPreconfigured.mockResolvedValue({ status: 404 });

        // Render hook with mock StorefrontInstanceContext
        const { result } = renderHook(() => useRecommendations({ preconfigured: true }), { wrapper });

        // Wait for hook to return the correct unit
        await wait(() => {
            expect(result.current).toStrictEqual({
                loading: false,
                units: null
            });
        });

        // Check MSE calls
        expect(mse.publish.recsRequestSent).toHaveBeenCalledTimes(1);
    });

    it('provides a preconfigured recommendation', async () => {
        // Prepare mocks
        mockFetchPreconfigured.mockResolvedValue({ status: 200, data: { results: units } });

        // Render hook with mock StorefrontInstanceContext
        const { result } = renderHook(() => useRecommendations({ preconfigured: true }), { wrapper });

        // Wait for hook to return the correct unit
        await wait(() => {
            expect(result.current).toStrictEqual({
                loading: false,
                units
            });
        });

        // Expect SDK constructor and method to be called
        expect(RecommendationsClient).toHaveBeenCalledTimes(1);
        expect(mockFetchPreconfigured).toHaveBeenCalledTimes(1);
        expect(mockFetch).not.toHaveBeenCalled();

        // Check MSE calls
        expect(mse.publish.recsRequestSent).toHaveBeenCalledTimes(1);
        expect(mse.context.setRecommendations).toHaveBeenCalledWith({ units });
        expect(mse.publish.recsResponseReceived).toHaveBeenCalledTimes(1);
    });

    it.each([
        ['no', '', ''],
        ['categoryInclusions', '4,5,6', 'categories: (4 OR 5 OR 6)'],
        ['categoryExclusions', '12', '-categories: (12)'],
        ['excludeMaxPrice', '100', 'prices.minimum.final: >100'],
        ['excludeMinPrice', '200', 'prices.maximum.final: <200'],
        ['includeMaxPrice', '10', 'prices.maximum.final: <10'],
        ['includeMinPrice', '20', 'prices.minimum.final: >20']
    ])('registers a recommendation with %s filter', async (filter, value, expected) => {
        // Prepare mocks
        mockFetch.mockResolvedValue({ status: 200, data: { units } });

        // Render hook with mock StorefrontInstanceContext
        const { result } = renderHook(
            () =>
                useRecommendations({
                    title: 'My Recommendation',
                    recommendationType: 'viewed-viewed',
                    [filter]: value
                }),
            { wrapper }
        );

        // Wait for hook to return the correct unit
        await wait(() => {
            expect(result.current).toStrictEqual({
                loading: false,
                units
            });
        });

        // Expect SDK constructor and method to be called
        expect(RecommendationsClient).toHaveBeenCalledTimes(1);
        expect(mockFetch).toHaveBeenCalledTimes(1);
        expect(mockRegister).toHaveBeenCalledWith({
            name: 'My Recommendation',
            type: 'viewed-viewed',
            filter: expected
        });
        expect(mockFetchPreconfigured).not.toHaveBeenCalled();

        // Check MSE calls
        expect(mse.publish.recsRequestSent).toHaveBeenCalledTimes(1);
        expect(mse.context.setRecommendations).toHaveBeenCalledWith({ units });
        expect(mse.publish.recsResponseReceived).toHaveBeenCalledTimes(1);
    });

    it('registers a recommendation with multiple filters', async () => {
        // Prepare mocks
        mockFetch.mockResolvedValue({ status: 200, data: { units } });

        // Render hook with mock StorefrontInstanceContext
        const { result } = renderHook(
            () =>
                useRecommendations({
                    title: 'My Recommendation',
                    recommendationType: 'viewed-viewed',
                    includeMaxPrice: '50.0',
                    includeMinPrice: '10.0'
                }),
            { wrapper }
        );

        // Wait for hook to return the correct unit
        await wait(() => {
            expect(result.current).toStrictEqual({
                loading: false,
                units
            });
        });

        // Expect SDK constructor and method to be called
        expect(RecommendationsClient).toHaveBeenCalledTimes(1);
        expect(mockFetch).toHaveBeenCalledTimes(1);
        expect(mockRegister).toHaveBeenCalledWith({
            name: 'My Recommendation',
            type: 'viewed-viewed',
            filter: 'prices.minimum.final: >10.0 AND product.myStoreViewCode.prices.maximum.final: <50.0'
        });
        expect(mockFetchPreconfigured).not.toHaveBeenCalled();

        // Check MSE calls
        expect(mse.publish.recsRequestSent).toHaveBeenCalledTimes(1);
        expect(mse.context.setRecommendations).toHaveBeenCalledWith({ units });
        expect(mse.publish.recsResponseReceived).toHaveBeenCalledTimes(1);
    });
});
