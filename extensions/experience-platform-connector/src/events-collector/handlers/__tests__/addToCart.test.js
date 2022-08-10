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
import handler from '../addToCart';
import { addSimpleProductEvent, addConfigurableProductEvent } from './__mocks__/cartAddItem';

describe('canHandle()', () => {
    it('returns true for the correct event type', () => {
        expect(handler.canHandle(addSimpleProductEvent)).toBeTruthy();
    });

    it('returns false for non supported event types', () => {
        const mockEvent = {
            type: 'USER_SIGN_OUT',
            payload: {}
        };
        expect(handler.canHandle(mockEvent)).toBeFalsy();
    });
});

describe('handle()', () => {
    it('calls the correct sdk functions with the correct context value', () => {
        const mockSdk = {
            context: {
                setShoppingCart: jest.fn()
            },
            publish: {
                addToCart: jest.fn()
            }
        };

        handler.handle(mockSdk, addConfigurableProductEvent);

        expect(mockSdk.context.setShoppingCart).toHaveBeenCalledTimes(1);
        expect(mockSdk.context.setShoppingCart.mock.calls[0][0]).toMatchInlineSnapshot(`
            Object {
              "giftMessageSelected": false,
              "giftWrappingSelected": false,
              "id": "kAR5Gg6uPC6J5wGY0ebecyKfX905epmU",
              "items": Array [
                Object {
                  "prices": Object {
                    "price": Object {
                      "currency": "USD",
                      "value": 78,
                    },
                  },
                  "product": Object {
                    "configurableOptions": Array [
                      Object {
                        "optionLabel": undefined,
                        "valueLabel": undefined,
                      },
                    ],
                    "name": "Johanna Skirt",
                    "sku": "VSK03",
                  },
                },
              ],
              "possibleOnepageCheckout": false,
              "prices": Object {
                "subtotalExcludingTax": Object {
                  "currency": "USD",
                  "value": 78,
                },
              },
            }
        `);

        expect(mockSdk.publish.addToCart).toHaveBeenCalledTimes(1);
    });
});
