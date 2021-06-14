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
import { renderHook } from '@testing-library/react-hooks';

import mockMagentoStorefrontEvents from '../mocks/mockMagentoStorefrontEvents';
import useCustomUrlEvent from '../useCustomUrlEvent';

describe('useCustomUrlEvent', () => {
    let mse;

    beforeAll(() => {
        window.document.body.setAttributeNode(document.createAttribute('data-cmp-data-layer-enabled'));
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;
    });

    beforeEach(() => {
        delete window.location;
        window.magentoStorefrontEvents.mockClear();
    });

    it('sends an event when the location is set', () => {
        window.location = new URL('http://localhost/page');

        renderHook(() => useCustomUrlEvent());

        expect(mse.context.setCustomUrl).toHaveBeenCalledWith({ customUrl: 'http://localhost/page' });
        expect(mse.publish.customUrl).toHaveBeenCalledTimes(1);
    });

    it('does not send an event when the location is not set', () => {
        renderHook(() => useCustomUrlEvent());

        expect(mse.context.setCustomUrl).not.toHaveBeenCalled();
        expect(mse.publish.customUrl).not.toHaveBeenCalled();
    });
});
