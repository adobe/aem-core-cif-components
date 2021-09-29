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
jest.mock('@apollo/client', () => ({
    ...jest.requireActual('@apollo/client'),
    useMutation: jest.fn()
}));

import React from 'react';
import { render, fireEvent, wait } from 'test-utils';

import AddToCart from '../addToCart';

import { useMutation } from '@apollo/client';

const simpleItem = {
    sku: 'simple-sku',
    quantity: 1
};

const bundleItem = {
    sku: 'bundle-sku',
    quantity: 2,
    bundle: true,
    options: [
        {
            foo: 'bar'
        }
    ]
};

const virtualItem = {
    sku: 'virtual-sku',
    quantity: 3,
    virtual: true
};

const dispatchEventSpy = jest.spyOn(document, 'dispatchEvent');

const addToCartMutationMock = jest.fn();
const addBundleToCartMutationMock = jest.fn();
const addVirtualToCartMutationMock = jest.fn();
const addSimpleAndVirtualToCartMutationMock = jest.fn();

jest.mock('../../../queries/mutation_add_to_cart.graphql', () => 'addToCart');
jest.mock('../../../queries/mutation_add_bundle_to_cart.graphql', () => 'addBundleToCart');
jest.mock('../../../queries/mutation_add_virtual_to_cart.graphql', () => 'addVirtualToCart');
jest.mock('../../../queries/mutation_add_simple_and_virtual_to_cart.graphql', () => 'addSimpleAndVirtualToCart');

useMutation.mockImplementation(mutation => {
    const { useMutation: actualUseMutation } = jest.requireActual('@apollo/client');
    if (mutation === 'addToCart') {
        return [addToCartMutationMock];
    } else if (mutation === 'addBundleToCart') {
        return [addBundleToCartMutationMock];
    } else if (mutation === 'addVirtualToCart') {
        return [addVirtualToCartMutationMock];
    } else if (mutation === 'addSimpleAndVirtualToCart') {
        return [addSimpleAndVirtualToCartMutationMock];
    } else {
        return actualUseMutation(mutation);
    }
});

describe('<AddToCart>', () => {
    afterEach(() => {
        dispatchEventSpy.mockClear();
        addToCartMutationMock.mockClear();
        addBundleToCartMutationMock.mockClear();
        addVirtualToCartMutationMock.mockClear();
        addSimpleAndVirtualToCartMutationMock.mockClear();
    });

    it('renders the component for empty items', () => {
        const { asFragment, getByRole } = render(<AddToCart items={[]} />);

        expect(asFragment()).toMatchSnapshot();
        expect(getByRole('button')).toBeDisabled();
    });

    it('renders the component with disabled property', () => {
        const { asFragment, getByRole } = render(<AddToCart items={[simpleItem]} disabled />);

        expect(asFragment()).toMatchSnapshot();
        expect(getByRole('button')).toBeDisabled();
    });

    it('renders the component with simple item', () => {
        const { asFragment, getByRole } = render(<AddToCart items={[simpleItem]} />);

        expect(asFragment()).toMatchSnapshot();
        expect(getByRole('button')).not.toBeDisabled();
    });

    it('add a simple product to cart', async () => {
        const { getByRole } = render(<AddToCart items={[simpleItem]} />);

        fireEvent.click(getByRole('button'));
        expect(addToCartMutationMock).toHaveBeenCalledTimes(1);
        expect(addToCartMutationMock).toHaveBeenCalledWith({
            variables: {
                cartId: null,
                cartItems: [
                    {
                        data: {
                            sku: simpleItem.sku,
                            quantity: parseFloat(simpleItem.quantity)
                        }
                    }
                ]
            }
        });
        await wait(() => {
            expect(dispatchEventSpy).toHaveBeenCalledTimes(1);
            expect(dispatchEventSpy).toHaveBeenCalledWith(new CustomEvent('aem.cif.after-add-to-cart'));
        });
    });

    it('add a bundle product to cart', async () => {
        const { getByRole } = render(<AddToCart items={[bundleItem]} />);

        fireEvent.click(getByRole('button'));
        expect(addBundleToCartMutationMock).toHaveBeenCalledTimes(1);
        expect(addBundleToCartMutationMock).toHaveBeenCalledWith({
            variables: {
                cartId: null,
                cartItems: [
                    {
                        data: {
                            sku: bundleItem.sku,
                            quantity: parseFloat(bundleItem.quantity)
                        },
                        bundle_options: bundleItem.options
                    }
                ]
            }
        });
        await wait(() => {
            expect(dispatchEventSpy).toHaveBeenCalledTimes(1);
            expect(dispatchEventSpy).toHaveBeenCalledWith(new CustomEvent('aem.cif.after-add-to-cart'));
        });
    });

    it('add a virtual product to cart', async () => {
        const { getByRole } = render(<AddToCart items={[virtualItem]} />);

        fireEvent.click(getByRole('button'));
        expect(addVirtualToCartMutationMock).toHaveBeenCalledTimes(1);
        expect(addVirtualToCartMutationMock).toHaveBeenCalledWith({
            variables: {
                cartId: null,
                cartItems: [
                    {
                        data: {
                            sku: virtualItem.sku,
                            quantity: parseFloat(virtualItem.quantity)
                        }
                    }
                ]
            }
        });
        await wait(() => {
            expect(dispatchEventSpy).toHaveBeenCalledTimes(1);
            expect(dispatchEventSpy).toHaveBeenCalledWith(new CustomEvent('aem.cif.after-add-to-cart'));
        });
    });

    it('add a simple product and a virtual product to cart', async () => {
        const { getByRole } = render(<AddToCart items={[simpleItem, virtualItem]} />);

        fireEvent.click(getByRole('button'));
        expect(addSimpleAndVirtualToCartMutationMock).toHaveBeenCalledTimes(1);
        expect(addSimpleAndVirtualToCartMutationMock).toHaveBeenCalledWith({
            variables: {
                cartId: null,
                virtualCartItems: [
                    {
                        data: {
                            sku: virtualItem.sku,
                            quantity: parseFloat(virtualItem.quantity)
                        }
                    }
                ],
                simpleCartItems: [
                    {
                        data: {
                            sku: simpleItem.sku,
                            quantity: parseFloat(simpleItem.quantity)
                        }
                    }
                ]
            }
        });
        await wait(() => {
            expect(dispatchEventSpy).toHaveBeenCalledTimes(1);
            expect(dispatchEventSpy).toHaveBeenCalledWith(new CustomEvent('aem.cif.after-add-to-cart'));
        });
    });

    it('add a simple product and provides callback', async () => {
        const onAddToCartMock = jest.fn();
        const { getByRole } = render(<AddToCart items={[simpleItem]} onAddToCart={onAddToCartMock} />);

        fireEvent.click(getByRole('button'));
        await wait(() => {
            expect(dispatchEventSpy).toHaveBeenCalledTimes(0);
            expect(onAddToCartMock).toHaveBeenCalledTimes(1);
            expect(onAddToCartMock).toHaveBeenCalledWith([simpleItem]);
        });
    });
});
