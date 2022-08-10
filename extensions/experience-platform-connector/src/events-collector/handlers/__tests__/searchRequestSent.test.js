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
import handler from '../searchRequestSent';
import { searchRequestEvent, searchbarRequestEvent } from './__mocks__/searchRequestSent';

describe('canHandle()', () => {
    it('searchRequestEvent returns true for the correct event type', () => {
        expect(handler.canHandle(searchRequestEvent)).toBeTruthy();
    });

    it('searchbarRequestEvent returns true for the correct event type', () => {
        expect(handler.canHandle(searchbarRequestEvent)).toBeTruthy();
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
    it('calls the correct sdk functions with the correct context value - 1', () => {
        const mockSdk = {
            context: {
                setSearchInput: jest.fn()
            },
            publish: {
                searchRequestSent: jest.fn()
            }
        };

        handler.handle(mockSdk, searchRequestEvent);

        expect(mockSdk.context.setSearchInput).toHaveBeenCalledTimes(1);
        expect(mockSdk.context.setSearchInput.mock.calls[0][0]).toMatchInlineSnapshot(`
            Object {
              "units": Array [
                Object {
                  "currentPage": 1,
                  "filter": Array [
                    Object {
                      "attribute": "category_id",
                      "in": Array [
                        "Bottoms,11",
                      ],
                    },
                    Object {
                      "attribute": "fashion_color",
                      "in": Array [
                        "Rain,34",
                        "Mint,25",
                      ],
                    },
                  ],
                  "pageSize": 12,
                  "phrase": "selena",
                  "queryTypes": Array [
                    "products",
                  ],
                  "searchUnitId": "productPage",
                  "sort": Array [
                    Object {
                      "attribute": "relevance",
                      "direction": "DESC",
                    },
                  ],
                },
              ],
            }
        `);

        expect(mockSdk.publish.searchRequestSent).toHaveBeenCalledTimes(1);
    });

    it('calls the correct sdk functions with the correct context value - 2', () => {
        const mockSdk = {
            context: {
                setSearchInput: jest.fn()
            },
            publish: {
                searchRequestSent: jest.fn()
            }
        };

        handler.handle(mockSdk, searchbarRequestEvent);

        expect(mockSdk.context.setSearchInput).toHaveBeenCalledTimes(1);
        expect(mockSdk.context.setSearchInput.mock.calls[0][0]).toMatchInlineSnapshot(`
            Object {
              "units": Array [
                Object {
                  "currentPage": 1,
                  "filter": Array [],
                  "pageSize": 3,
                  "phrase": "selena",
                  "queryTypes": Array [
                    "products",
                  ],
                  "searchUnitId": "productPage",
                  "sort": Array [
                    Object {
                      "attribute": undefined,
                      "direction": undefined,
                    },
                  ],
                },
              ],
            }
        `);

        expect(mockSdk.publish.searchRequestSent).toHaveBeenCalledTimes(1);
    });
});
