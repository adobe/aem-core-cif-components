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

import React from 'react';
import { useMutation } from '@apollo/client';
import { fireEvent, wait } from '@testing-library/react';

import { render } from '../../../utils/test-utils';
import useCartOptions from '../useCartOptions';
import CART_DETAILS_QUERY from '../../../queries/query_cart_details.graphql';
import MUTATION_UPDATE_CART_ITEM from '../../../queries/mutation_update_cart_item.graphql';
import { useAwaitQuery } from '../../../utils/hooks';
import { CartProvider } from '../cartContext';
import mockMagentoStorefrontEvents from '../../../utils/mocks/mockMagentoStorefrontEvents';

const mocks = [
    {
        request: {
            query: MUTATION_UPDATE_CART_ITEM,
            variables: { cartId: null, cartItemUid: undefined, quantity: 2 }
        },
        result: {
            data: {
                cart: {
                    items: [
                        {
                            uid: 'MTM5Mg==',
                            quantity: 3,
                            product: {
                                name: 'lucky-pants'
                            }
                        }
                    ]
                }
            }
        }
    }
];
describe('useCartOptions', () => {
    let mse;

    const MockComponent = () => {
        const [updateCartItemMutation] = useMutation(MUTATION_UPDATE_CART_ITEM);
        const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);
        const [, { updateCartItem }] = useCartOptions({
            updateCartItemMutation,
            cartDetailsQuery
        });

        return (
            <div>
                <button onClick={() => updateCartItem(2)}>Update Quantity</button>
            </div>
        );
    };

    beforeAll(() => {
        mse = window.magentoStorefrontEvents = mockMagentoStorefrontEvents;
    });

    beforeEach(() => {
        window.magentoStorefrontEvents.mockClear();
    });

    it('publishes an updateCart event', async () => {
        const { getByRole } = render(
            <CartProvider>
                <MockComponent />
            </CartProvider>,
            { mocks }
        );

        fireEvent.click(getByRole('button'));

        await wait(() => expect(mse.publish.updateCart).toHaveBeenCalledTimes(1));
    });
});
