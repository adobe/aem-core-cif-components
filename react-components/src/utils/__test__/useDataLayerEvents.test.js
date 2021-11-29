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
});
