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
import { act, renderHook } from '@testing-library/react-hooks';

import usePageEvent from '../usePageEvent';
import mockMagentoStorefrontEvents from '../mocks/mockMagentoStorefrontEvents';

describe('usePageEvent', () => {
    let mse;
    let component;

    beforeAll(() => {
        window.document.body.setAttributeNode(document.createAttribute('data-cmp-data-layer-enabled'));
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;
    });

    beforeEach(() => {
        window.magentoStorefrontEvents.mockClear();
        component = document.createElement('div');
        document.body.appendChild(component);
    });

    afterEach(() => {
        component.parentElement.removeChild(component);
    });

    it('sends a PageBuilder pageView event', () => {
        renderHook(() => usePageEvent());
        act(() => {
            window.dispatchEvent(new CustomEvent('beforeunload'));
        });

        expect(mse.context.setPage).toHaveBeenCalledTimes(1);
        expect(mse.context.setPage.mock.calls[0][0]).toMatchObject({
            pageType: 'PageBuilder',
            eventType: 'pageUnload',
            ping_interval: 0,
            pings: 0
        });
        expect(mse.publish.pageView).toHaveBeenCalledTimes(1);
    });

    it('sends a Product pageView event', () => {
        component.dataset['cifProductContext'] = '';

        renderHook(() => usePageEvent());
        act(() => {
            window.dispatchEvent(new CustomEvent('beforeunload'));
        });

        expect(mse.context.setPage).toHaveBeenCalledTimes(1);
        expect(mse.context.setPage.mock.calls[0][0]).toMatchObject({
            pageType: 'Product',
            eventType: 'pageUnload',
            ping_interval: 0,
            pings: 0
        });
        expect(mse.publish.pageView).toHaveBeenCalledTimes(1);
    });

    it('sends a Category pageView event', () => {
        component.dataset['cifCategoryContext'] = '';

        renderHook(() => usePageEvent());
        act(() => {
            window.dispatchEvent(new CustomEvent('beforeunload'));
        });

        expect(mse.context.setPage).toHaveBeenCalledTimes(1);
        expect(mse.context.setPage.mock.calls[0][0]).toMatchObject({
            pageType: 'Category',
            eventType: 'pageUnload',
            ping_interval: 0,
            pings: 0
        });
        expect(mse.publish.pageView).toHaveBeenCalledTimes(1);
    });
});
