/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
import handler from '../shoppingMiniCartView';

import miniCartViewEvent from './__mocks__/miniCartView';
import cartPageViewEvent from './__mocks__/cartPageView';

describe('canHandle()', () => {
    it('returns true for the correct event type', () => {
        expect(handler.canHandle(miniCartViewEvent)).toBeTruthy();
    });

    it('returns false for non supported event types', () => {
        expect(handler.canHandle(cartPageViewEvent)).toBeFalsy();
    });
});

describe('handle()', () => {
    it('calls the correct sdk functions with the correct context value', () => {
        const mockSdk = {
            context: {
                setShoppingCart: jest.fn()
            },
            publish: {
                shoppingCartView: jest.fn()
            }
        };

        handler.handle(mockSdk, miniCartViewEvent);

        expect(mockSdk.context.setShoppingCart).toHaveBeenCalledTimes(1);
        expect(mockSdk.context.setShoppingCart.mock.calls[0][0]).toMatchSnapshot();

        expect(mockSdk.publish.shoppingCartView).toHaveBeenCalledTimes(1);
    });
});
