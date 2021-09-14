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

/* eslint-disable react/prop-types */

import React from 'react';
import { fireEvent, wait } from '@testing-library/react';
import { act } from 'react-dom/test-utils';

const { TextEncoder } = require('util');
const { Crypto } = require('@peculiar/webcrypto');

import mockMagentoStorefrontEvents from '../../../utils/mocks/mockMagentoStorefrontEvents';
import { render } from '../../../utils/test-utils';
import { CartProvider } from '../cartContext';
import useProduct from '../useProduct';

describe('useProduct', () => {
    let mse;

    const MockComponent = props => {
        const { item } = props;

        const [, { removeItem }] = useProduct({ item });
        return (
            <div>
                <button
                    onClick={() => {
                        removeItem(item.uid);
                    }}>
                    Remove
                </button>
            </div>
        );
    };

    beforeAll(() => {
        window.TextEncoder = TextEncoder;
        window.crypto = new Crypto();

        window.document.body.setAttributeNode(document.createAttribute('data-cmp-data-layer-enabled'));
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;

        window.adobeDataLayer = [];
        window.adobeDataLayer.push = jest.fn();
    });

    beforeEach(() => {
        window.adobeDataLayer.push.mockClear();
        window.magentoStorefrontEvents.mockClear();
    });

    it('publishes an removeFromCart event', async () => {
        const item = {
            uid: 'MTM5Mg==',
            quantity: 1,
            prices: {},
            product: {
                name: 'Honora Wide Leg Pants',
                sku: 'VP05-MT-S'
            }
        };
        const { getByRole } = render(
            <CartProvider>
                <MockComponent item={item} />
            </CartProvider>
        );

        await act(async () => fireEvent.click(getByRole('button')));
        await wait(() => {
            expect(mse.publish.removeFromCart).toHaveBeenCalledTimes(1);
            expect(window.adobeDataLayer.push).toHaveBeenCalledWith({
                event: 'cif:removeFromCart',
                eventInfo: {
                    '@id': 'product-c9da66dcca',
                    'xdm:SKU': 'VP05-MT-S',
                    'xdm:quantity': 1
                }
            });
        });
    });
});
