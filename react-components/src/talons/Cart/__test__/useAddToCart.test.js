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
import { render } from '../../../utils/test-utils';
import useAddToCart from '../useAddToCart';

jest.mock('@magento/peregrine/lib/context/cart', () => {
    const cartState = { cartId: 'TestCart' };
    const cartApi = {};
    const useCartContext = jest.fn(() => [cartState, cartApi]);
    return {
        __esModule: true,
        default: jest.requireActual('@magento/peregrine/lib/context/cart').default,
        useCartContext
    };
});

jest.mock('@apollo/client', () => ({
    ...jest.requireActual('@apollo/client'),
    useMutation: jest.fn().mockImplementation(() => [
        jest.fn(),
        {
            error: null
        }
    ])
}));

describe('useAddToCart', () => {
    const defaultHook = jest.fn().mockName('default');
    const customHook = jest.fn().mockName('custom');
    // when useMutation is called with a mock, return the mock otherwise return the default mock
    useMutation.mockImplementation(mutation => (mutation.mock ? [mutation] : [defaultHook]));

    const MockComponet = props => {
        const [{ cartId }, api] = useAddToCart(props);
        return (
            <div data-testid="test" data-cartid={cartId}>
                <button onClick={() => api.addPhysicalProductItems()}>addPhysicalProductItems</button>
                <button onClick={() => api.addVirtualProductItems()}>addVirtualProductItems</button>
                <button onClick={() => api.addPhysicalAndVirtualProductItems()}>
                    addPhysicalAndVirtualProductItems
                </button>
                <button onClick={() => api.addBundledProductItems()}>addBundledProductItems</button>
                <button onClick={() => api.addGiftCardProductItems()}>addGiftCardProductItems</button>
                <button onClick={() => api.addProductsToCart()}>addProductsToCart</button>
            </div>
        );
    };

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('returns cartId', () => {
        const { getByTestId } = render(<MockComponet />);
        const { cartid } = getByTestId('test').dataset;
        expect(cartid).toBe('TestCart');
    });

    it('calls the default mutations', () => {
        // when
        const { getByText } = render(<MockComponet />);
        getByText('addPhysicalProductItems').click();
        getByText('addVirtualProductItems').click();
        getByText('addPhysicalAndVirtualProductItems').click();
        getByText('addBundledProductItems').click();
        getByText('addGiftCardProductItems').click();
        getByText('addProductsToCart').click();

        // then
        expect(defaultHook).toHaveBeenCalledTimes(6);
        expect(customHook).toHaveBeenCalledTimes(0);
    });

    it('calls the custom mutations', () => {
        // when
        const operations = {
            addPhysicalProductItemsMutation: customHook,
            addBundledProductItemsMutation: customHook,
            addVirtualProductItemsMutation: customHook,
            addPhysicalAndVirtualProductItemsMutation: customHook,
            addGiftCardProductItemsMutation: customHook,
            addProductsToCartMutation: customHook
        };
        const { getByText } = render(<MockComponet operations={operations} />);
        getByText('addPhysicalProductItems').click();
        getByText('addVirtualProductItems').click();
        getByText('addPhysicalAndVirtualProductItems').click();
        getByText('addBundledProductItems').click();
        getByText('addGiftCardProductItems').click();
        getByText('addProductsToCart').click();

        // then
        expect(defaultHook).toHaveBeenCalledTimes(0);
        expect(customHook).toHaveBeenCalledTimes(6);
    });
});
