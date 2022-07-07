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
jest.mock('../dataLayerUtils', () => ({
    pushEvent: jest.fn()
}));
import { pushEvent } from '../dataLayerUtils';
import useDataLayerEvents from '../useDataLayerEvents';

describe('useDataLayerEvents', () => {
    beforeEach(() => {
        pushEvent.mockClear();
    });

    it('listens to add to cart event', () => {
        renderHook(() => useDataLayerEvents());
        act(() => {
            const customEvent = new CustomEvent('aem.cif.add-to-cart', {
                bubbles: true,
                detail: [{ productId: 'test-id', sku: 'test-sku', quantity: 1 }]
            });
            document.dispatchEvent(customEvent);
        });

        expect(pushEvent).toHaveBeenCalledTimes(1);
        expect(pushEvent).toHaveBeenCalledWith('cif:addToCart', {
            '@id': 'test-id',
            'xdm:SKU': 'test-sku',
            'xdm:quantity': 1
        });
    });

    it('listens to add to wishlist event', () => {
        renderHook(() => useDataLayerEvents());
        act(() => {
            const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
                bubbles: true,
                detail: [{ productId: 'test-id', sku: 'test-sku', quantity: 1 }]
            });
            document.dispatchEvent(customEvent);
        });

        expect(pushEvent).toHaveBeenCalledTimes(1);
        expect(pushEvent).toHaveBeenCalledWith('cif:addToWishList', {
            '@id': 'test-id',
            'xdm:SKU': 'test-sku',
            'xdm:quantity': 1
        });
    });

    it('includes the event targets component id as path', () => {
        const button = document.createElement('button');
        button.dataset.cmpDataLayer = JSON.stringify({ 'component-id-1234': {} });
        document.body.appendChild(button);

        renderHook(() => useDataLayerEvents());
        act(() => {
            const bubbles = true;
            const detail = [{ productId: 'test-id', sku: 'test-sku', quantity: 1 }];
            let customEvent = new CustomEvent('aem.cif.add-to-cart', { bubbles, detail });
            button.dispatchEvent(customEvent);

            customEvent = new CustomEvent('aem.cif.add-to-wishlist', { bubbles, detail });
            button.dispatchEvent(customEvent);
        });

        expect(pushEvent).toHaveBeenCalledWith('cif:addToCart', {
            '@id': 'test-id',
            'xdm:SKU': 'test-sku',
            'xdm:quantity': 1,
            path: 'component.component-id-1234'
        });
        expect(pushEvent).toHaveBeenCalledWith('cif:addToWishList', {
            '@id': 'test-id',
            'xdm:SKU': 'test-sku',
            'xdm:quantity': 1,
            path: 'component.component-id-1234'
        });
    });

    it('includes the event targets ancestors component id as path', () => {
        const component = document.createElement('div');
        const button = document.createElement('button');
        component.dataset.cmpDataLayer = JSON.stringify({ 'parent-component-id-1234': {} });
        component.appendChild(button);
        document.body.appendChild(component);

        renderHook(() => useDataLayerEvents());
        act(() => {
            const bubbles = true;
            const detail = [{ productId: 'test-id', sku: 'test-sku', quantity: 1 }];
            let customEvent = new CustomEvent('aem.cif.add-to-cart', { bubbles, detail });
            button.dispatchEvent(customEvent);

            customEvent = new CustomEvent('aem.cif.add-to-wishlist', { bubbles, detail });
            button.dispatchEvent(customEvent);
        });

        expect(pushEvent).toHaveBeenCalledWith('cif:addToCart', {
            '@id': 'test-id',
            'xdm:SKU': 'test-sku',
            'xdm:quantity': 1,
            path: 'component.parent-component-id-1234'
        });
        expect(pushEvent).toHaveBeenCalledWith('cif:addToWishList', {
            '@id': 'test-id',
            'xdm:SKU': 'test-sku',
            'xdm:quantity': 1,
            path: 'component.parent-component-id-1234'
        });
    });
});
