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

import { useEffect, useRef } from 'react';

const documentScrollTop = () => document.body.scrollTop || document.documentElement.scrollTop;
const documentScrollLeft = () => document.body.scrollLeft || document.documentElement.scrollLeft;

const useViewOffsets = () => {
    const minXOffset = useRef(0);
    const maxXOffset = useRef(0);
    const minYOffset = useRef(0);
    const maxYOffset = useRef(0);

    const onResizeOrScroll = () => {
        const currentXOffset = documentScrollLeft();
        const currentYOffset = documentScrollTop();
        minXOffset.current = Math.min(currentXOffset, minXOffset.current);
        maxXOffset.current = Math.max(currentXOffset, maxXOffset.current);
        minYOffset.current = Math.min(currentYOffset, minYOffset.current);
        maxYOffset.current = Math.max(currentYOffset, maxYOffset.current);
    };

    useEffect(() => {
        const currentXOffset = documentScrollLeft();
        const currentYOffset = documentScrollTop();
        minXOffset.current = currentXOffset;
        maxXOffset.current = currentXOffset + window.innerWidth;
        minYOffset.current = currentYOffset;
        maxYOffset.current = currentYOffset + window.innerHeight;

        window.addEventListener('scroll', onResizeOrScroll);
        window.addEventListener('resize', onResizeOrScroll);
        return () => {
            window.removeEventListener('scroll', onResizeOrScroll);
            window.removeEventListener('resize', onResizeOrScroll);
        };
    }, []);

    return { minXOffset, maxXOffset, minYOffset, maxYOffset };
};

export default useViewOffsets;
