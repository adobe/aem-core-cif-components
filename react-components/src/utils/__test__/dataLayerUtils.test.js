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
import * as dataLayerUtils from '../dataLayerUtils';
const { TextEncoder } = require('util');
const { Crypto } = require('@peculiar/webcrypto');

describe('DataLayer utilities', () => {
    const sampleCart = {
        id: 'jg7LPugoxb4Av97cIgf80XL0sRPCaZ8Y',
        is_virtual: false,
        prices: {
            subtotal_excluding_tax: {
                currency: 'USD',
                value: 78,
                __typename: 'Money'
            },
            subtotal_including_tax: {
                currency: 'USD',
                value: 78,
                __typename: 'Money'
            },
            __typename: 'CartPrices'
        },
        email: 'my-private@email.com',
        total_quantity: 1,
        items: [
            {
                __typename: 'SimpleCartItem',
                uid: 'MTM5Mg==',
                quantity: 1,
                prices: {
                    price: {
                        currency: 'USD',
                        value: 78,
                        __typename: 'Money'
                    },
                    __typename: 'CartItemPrices'
                },
                product: {
                    name: 'Honora Wide Leg Pants',
                    sku: 'VP05-MT-S',
                    __typename: 'SimpleProduct'
                }
            }
        ],
        __typename: 'Cart'
    };

    beforeAll(() => {
        window.TextEncoder = TextEncoder;
        window.crypto = new Crypto();

        window.document.body.setAttributeNode(document.createAttribute('data-cmp-data-layer-enabled'));

        window.adobeDataLayer = [];
        window.adobeDataLayer.getState = jest.fn(ref => {
            return { result: ref };
        });
        window.adobeDataLayer.push = jest.fn();
        window.adobeDataLayer.addEventListener = jest.fn();
        window.adobeDataLayer.removeEventListener = jest.fn();
    });

    beforeEach(() => {
        window.adobeDataLayer.getState.mockClear();
        window.adobeDataLayer.push.mockClear();
        window.adobeDataLayer.addEventListener.mockClear();
        window.adobeDataLayer.removeEventListener.mockClear();
    });

    it('pushes data', () => {
        dataLayerUtils.pushData({ test: 'data' });
        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(1);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({ test: 'data' });
    });

    it('pushes event', () => {
        dataLayerUtils.pushEvent('test-event', { test: 'data' });
        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(1);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'test-event',
            eventInfo: { test: 'data' }
        });
    });

    it('gets state', () => {
        dataLayerUtils.getState('data');
        expect(window.adobeDataLayer.getState).toHaveBeenCalledTimes(1);
        expect(window.adobeDataLayer.getState).toHaveBeenCalledWith('data');
        expect(window.adobeDataLayer.getState).toHaveReturnedWith({ result: 'data' });
    });

    it('adds event listener', () => {
        dataLayerUtils.addEventListener('event', 'listener');
        expect(window.adobeDataLayer.addEventListener).toHaveBeenCalledTimes(1);
        expect(window.adobeDataLayer.addEventListener).toHaveBeenCalledWith('event', 'listener');
    });

    it('removes event listener', () => {
        dataLayerUtils.removeEventListener('event', 'listener');
        expect(window.adobeDataLayer.removeEventListener).toHaveBeenCalledTimes(1);
        expect(window.adobeDataLayer.removeEventListener).toHaveBeenCalledWith('event', 'listener');
    });

    it('generates dataLayer ID', async () => {
        const result = await dataLayerUtils.generateDataLayerId('product', 'SKU-24');
        expect(result).toBe('product-06851b6172');
    });

    it('transforms a GraphQL response to camelCase', () => {
        const result = dataLayerUtils.transformGraphqlResponse(sampleCart);
        expect(result).toMatchSnapshot();
    });

    it('transforms a cart', () => {
        const cart = dataLayerUtils.transformGraphqlResponse(sampleCart);
        const result = dataLayerUtils.transformCart(cart);
        expect(result).toMatchSnapshot();
    });
});
