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

import useViewedOffsets from '../useViewedOffsets';

describe('useViewedOffsets', () => {
    it('provides initial values', () => {
        document.body.scrollTop = 0;
        document.body.scrollLeft = 0;
        window.innerWidth = 500;
        window.innerHeight = 500;

        const { result } = renderHook(() => useViewedOffsets());
        const { minXOffset, maxXOffset, minYOffset, maxYOffset } = result.current;

        expect(minXOffset.current).toBe(0);
        expect(maxXOffset.current).toBe(500);
        expect(minYOffset.current).toBe(0);
        expect(maxYOffset.current).toBe(500);
    });

    it('acts on scroll events', () => {
        document.body.scrollTop = 0;
        document.body.scrollLeft = 0;
        window.innerWidth = 500;
        window.innerHeight = 500;

        const { result } = renderHook(() => useViewedOffsets());

        act(() => {
            document.body.scrollTop = 250;
            document.body.scrollLeft = 250;
            window.dispatchEvent(new CustomEvent('scroll'));
        });

        const { minXOffset, maxXOffset, minYOffset, maxYOffset } = result.current;
        expect(minXOffset.current).toBe(0);
        expect(maxXOffset.current).toBe(750);
        expect(minYOffset.current).toBe(0);
        expect(maxYOffset.current).toBe(750);
    });

    it('acts on resize events', () => {
        document.body.scrollTop = 0;
        document.body.scrollLeft = 0;
        window.innerWidth = 500;
        window.innerHeight = 500;

        const { result } = renderHook(() => useViewedOffsets());

        act(() => {
            window.innerWidth = 1000;
            window.innerHeight = 1000;
            window.dispatchEvent(new CustomEvent('resize'));
        });

        const { minXOffset, maxXOffset, minYOffset, maxYOffset } = result.current;
        expect(minXOffset.current).toBe(0);
        expect(maxXOffset.current).toBe(1000);
        expect(minYOffset.current).toBe(0);
        expect(maxYOffset.current).toBe(1000);
    });
});
