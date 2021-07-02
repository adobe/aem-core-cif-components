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

import { useEffect } from 'react';

import { usePageType, useStorefrontEvents } from './hooks';
import useViewedOffsets from './useViewedOffsets';

const usePageEvent = () => {
    const mse = useStorefrontEvents();
    const { minXOffset, maxXOffset, minYOffset, maxYOffset } = useViewedOffsets();
    const pageType = usePageType();

    const sendPageEvent = () => {
        const context = {
            pageType,
            eventType: 'pageUnload',
            maxXOffset: maxXOffset.current,
            maxYOffset: maxYOffset.current,
            minXOffset: minXOffset.current,
            minYOffset: minYOffset.current,
            ping_interval: 0,
            pings: 0
        };

        mse.context.setPage(context);
        mse.publish.pageView();
    };

    useEffect(() => {
        if (!mse) {
            return;
        }

        window.addEventListener('beforeunload', sendPageEvent);
        return () => {
            window.removeEventListener('beforeunload', sendPageEvent);
        };
    }, []);
};

export default usePageEvent;
