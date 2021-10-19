/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
/* eslint-disable react/prop-types */

import React from 'react';
import { fireEvent, waitForElement } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import { act } from 'react-dom/test-utils';
import useMinicart from '../useMinicart';
import { CartProvider } from '../cartContext';
import mockMagentoStorefrontEvents from '../../../utils/mocks/mockMagentoStorefrontEvents';

describe('useMinicart', () => {
    let mse;

    const cartDetailsQuery = jest.fn(async args => {
        const cartId = args ? args.variables.cartId : 'guest123';
        return { data: { cart: { id: cartId, email: 'dummy@example.com' } } };
    });

    const addToCartMutation = jest.fn();
    const addVirtualItemMutation = jest.fn();
    const createCartMutation = jest.fn();
    const addSimpleAndVirtualItemMutation = jest.fn();
    const addBundleItemMutation = jest.fn();
    const addVirtualGiftcardItemMutation = jest.fn();

    const queries = {
        cartDetailsQuery,
        addToCartMutation,
        addVirtualItemMutation,
        addSimpleAndVirtualItemMutation,
        addBundleItemMutation,
        addVirtualGiftcardItemMutation,
        createCartMutation
    };

    const MockComponent = props => {
        const [data, api] = useMinicart({ queries });
        const { event } = props;

        const addToCart = async () => {
            await api.addItem(event);
        };
        return (
            <div>
                <div data-testid="cartDetails">{data.cart && data.cart.email}</div>
                <button onClick={addToCart}>Add to cart</button>
            </div>
        );
    };

    beforeAll(() => {
        window.document.body.setAttributeNode(document.createAttribute('data-cmp-data-layer-enabled'));
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;

        window.adobeDataLayer = [];
        window.adobeDataLayer.push = jest.fn();
    });

    beforeEach(() => {
        window.adobeDataLayer.push.mockClear();
        window.magentoStorefrontEvents.mockClear();
    });

    it('adds an item to cart', async () => {
        const mockEvent = {
            detail: [
                {
                    productId: 'test-id',
                    sku: '123',
                    quantity: 2,
                    giftcard: { entered_options: [], is_giftcard: false, type: '' }
                }
            ]
        };

        const { getByRole, getByTestId } = render(
            <CartProvider initialState={{ cartId: 'guest123' }}>
                <MockComponent event={mockEvent} />
            </CartProvider>
        );
        const cartIdNode = await waitForElement(() => getByTestId('cartDetails'));
        expect(cartIdNode.textContent).toEqual('dummy@example.com');
        expect(cartDetailsQuery).toHaveBeenCalledTimes(1);

        await act(async () => fireEvent.click(getByRole('button')));

        expect(addToCartMutation).toHaveBeenCalledTimes(1);

        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(2);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'cif:addToCart',
            eventInfo: {
                '@id': 'test-id',
                'xdm:SKU': '123',
                'xdm:quantity': 2
            }
        });
        expect(mse.publish.addToCart).toHaveBeenCalledTimes(1);
    });

    it('adds multiple items to cart', async () => {
        const mockEvent = {
            detail: [
                { sku: '4566', quantity: 2, giftcard: { entered_options: [], is_giftcard: false, type: '' } },
                {
                    sku: '123',
                    quantity: 3,
                    virtual: true,
                    giftcard: { entered_options: [], is_giftcard: false, type: '' }
                },
                { sku: '4566', quantity: 2, giftcard: { entered_options: [], is_giftcard: false, type: '' } }
            ]
        };

        const { getByRole } = render(
            <CartProvider initialState={{ cartId: 'guest123' }}>
                <MockComponent event={mockEvent} />
            </CartProvider>
        );

        await act(async () => fireEvent.click(getByRole('button')));

        expect(addSimpleAndVirtualItemMutation).toHaveBeenCalledTimes(1);
        expect(mse.publish.addToCart).toHaveBeenCalledTimes(1);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'cif:addToCart',
            eventInfo: { '@id': undefined, 'xdm:SKU': '4566', 'xdm:quantity': 2 }
        });
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'cif:addToCart',
            eventInfo: { '@id': undefined, 'xdm:SKU': '123', 'xdm:quantity': 3 }
        });
        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(4);
    });

    it('adds Bundle Product to cart', async () => {
        const mockEvent = {
            detail: [
                {
                    productId: 'test-id',
                    sku: '123',
                    virtual: false,
                    bundle: true,
                    giftcard: { entered_options: [], is_giftcard: false, type: '' },
                    quantity: 1,
                    options: {
                        id: 1,
                        quantity: 1,
                        value: 'option'
                    }
                }
            ]
        };

        const { getByRole } = render(
            <CartProvider initialState={{ cartId: 'guest123' }}>
                <MockComponent event={mockEvent} />
            </CartProvider>
        );

        await act(async () => fireEvent.click(getByRole('button')));

        expect(addBundleItemMutation).toHaveBeenCalledTimes(1);

        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(2);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'cif:addToCart',
            eventInfo: {
                '@id': 'test-id',
                'xdm:SKU': '123',
                'xdm:quantity': 1,
                bundle: true
            }
        });
        expect(mse.publish.addToCart).toHaveBeenCalledTimes(1);
    });

    it('adds Virtual Giftcard Product to cart', async () => {
        const mockEvent = {
            detail: [
                {
                    productId: 'test-id',
                    sku: '123',
                    virtual: false,
                    giftcard: {
                        is_giftcard: true,
                        type: 'VIRTUAL',
                        entered_options: [
                            {
                                uid: 'Z2lmdGNhcmQvY3VzdG9tX2dpZnRjYXJkX2Ftb3VudA==',
                                value: '-10.0'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=',
                                value: 'Sender'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls',
                                value: 'sender@example.com'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=',
                                value: 'Receiver'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls',
                                value: 'receiver@example.com'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==',
                                value: 'Enjoy!!'
                            }
                        ]
                    },
                    quantity: 1
                }
            ]
        };

        const { getByRole } = render(
            <CartProvider initialState={{ cartId: 'guest123' }}>
                <MockComponent event={mockEvent} />
            </CartProvider>
        );

        await act(async () => fireEvent.click(getByRole('button')));

        expect(addVirtualGiftcardItemMutation).toHaveBeenCalledTimes(1);

        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(2);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'cif:addToCart',
            eventInfo: {
                '@id': 'test-id',
                'xdm:SKU': '123',
                'xdm:quantity': 1
            }
        });
        expect(mse.publish.addToCart).toHaveBeenCalledTimes(1);
    });

    it('adds COMBINED Giftcard Product to cart', async () => {
        const mockEvent = {
            detail: [
                {
                    productId: 'test-id',
                    sku: '123',
                    virtual: false,
                    giftcard: {
                        is_giftcard: true,
                        type: 'COMBINED',
                        entered_options: [
                            {
                                uid: 'Z2lmdGNhcmQvY3VzdG9tX2dpZnRjYXJkX2Ftb3VudA==',
                                value: '20'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=',
                                value: 'Sender'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls',
                                value: 'sender@example.com'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=',
                                value: 'Receiver'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls',
                                value: 'receiver@example.com'
                            },
                            {
                                uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==',
                                value: 'Enjoy!!'
                            }
                        ]
                    },
                    quantity: 1
                }
            ]
        };

        const { getByRole } = render(
            <CartProvider initialState={{ cartId: 'guest123' }}>
                <MockComponent event={mockEvent} />
            </CartProvider>
        );

        await act(async () => fireEvent.click(getByRole('button')));

        expect(addVirtualGiftcardItemMutation).toHaveBeenCalledTimes(2);

        expect(window.adobeDataLayer.push).toHaveBeenCalledTimes(2);
        expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
            event: 'cif:addToCart',
            eventInfo: {
                '@id': 'test-id',
                'xdm:SKU': '123',
                'xdm:quantity': 1
            }
        });
        expect(mse.publish.addToCart).toHaveBeenCalledTimes(1);
    });
});
