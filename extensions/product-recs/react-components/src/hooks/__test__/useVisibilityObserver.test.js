/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
import { renderHook } from '@testing-library/react-hooks';

import { useVisibilityObserver } from '../useVisibilityObserver';

describe('useVisibilityObserver', () => {
    // Mock IntersectionObserver
    let mockCallback;
    let mockObserve = jest.fn();
    Object.defineProperty(window, 'IntersectionObserver', {
        writable: true,
        value: jest.fn().mockImplementation(callback => {
            mockCallback = callback;
            return {
                observe: mockObserve
            };
        })
    });

    it('triggers the callback when 50% of the observed element is in the viewport', () => {
        const element = <div />;
        const callback = jest.fn();

        const { result } = renderHook(() => useVisibilityObserver({ threshold: [0.0, 0.5] }));
        result.current.observeElement(element, callback);

        // Scroll in view
        mockCallback([{ isIntersecting: true, intersectionRatio: 0.0 }], callback);
        expect(callback).toHaveBeenCalledTimes(0);

        mockCallback([{ isIntersecting: true, intersectionRatio: 1.0 }], callback);
        expect(callback).toHaveBeenCalledTimes(1);

        // Scroll out of view
        mockCallback([{ isIntersecting: false, intersectionRatio: 0.1 }], callback);
        expect(callback).toHaveBeenCalledTimes(1);

        // Scroll in view again
        mockCallback([{ isIntersecting: true, intersectionRatio: 0.6 }], callback);
        expect(callback).toHaveBeenCalledTimes(2);
    });
});
