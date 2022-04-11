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
import { useCallback } from 'react';
import { useMutation } from '@apollo/client';
import mergeOperations from '@magento/peregrine/lib/util/shallowMerge';
import { useCartContext } from '@magento/peregrine/lib/context/cart';

import MUTATION_ADD_ITEMS_TO_CART from '../../queries/mutation_add_items_to_cart.graphql';
import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';
import MUTATION_ADD_BUNDLE_TO_CART from '../../queries/mutation_add_bundle_to_cart.graphql';
import MUTATION_ADD_VIRTUAL_TO_CART from '../../queries/mutation_add_virtual_to_cart.graphql';
import MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART from '../../queries/mutation_add_simple_and_virtual_to_cart.graphql';

const defaultOperations = {
    addProductsToCartMutation: MUTATION_ADD_ITEMS_TO_CART,
    addPhysicalProductItemsMutation: MUTATION_ADD_TO_CART,
    addBundledProductItemsMutation: MUTATION_ADD_BUNDLE_TO_CART,
    addVirtualProductItemsMutation: MUTATION_ADD_VIRTUAL_TO_CART,
    addGiftCardProductItemsMutation: MUTATION_ADD_ITEMS_TO_CART,
    addPhysicalAndVirtualProductItemsMutation: MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART
};

const useAddToCart = (props = {}) => {
    const operations = mergeOperations(defaultOperations, props.operations || {});
    const [addProductsToCart] = useMutation(operations.addProductsToCartMutation);
    const [addPhysicalProductItems] = useMutation(operations.addPhysicalProductItemsMutation);
    const [addBundledProductItems] = useMutation(operations.addBundledProductItemsMutation);
    const [addVirtualProductItems] = useMutation(operations.addVirtualProductItemsMutation);
    const [addGiftCardProductItems] = useMutation(operations.addGiftCardProductItemsMutation);
    const [addPhysicalAndVirtualProductItems] = useMutation(operations.addPhysicalAndVirtualProductItemsMutation);
    const [{ cartId }] = useCartContext();

    return [
        // data
        {
            cartId
        },
        // api
        {
            addProductsToCart: useCallback(
                async cartItems => {
                    await addProductsToCart({ variables: { cartId, cartItems } });
                },
                [cartId]
            ),
            addPhysicalProductItems: useCallback(
                async cartItems => {
                    await addPhysicalProductItems({ variables: { cartId, cartItems } });
                },
                [cartId]
            ),
            addBundledProductItems: useCallback(
                async cartItems => {
                    await addBundledProductItems({ variables: { cartId, cartItems } });
                },
                [cartId]
            ),
            addVirtualProductItems: useCallback(
                async cartItems => {
                    await addVirtualProductItems({ variables: { cartId, cartItems } });
                },
                [cartId]
            ),
            addGiftCardProductItems: useCallback(
                async cartItems => {
                    await addGiftCardProductItems({ variables: { cartId, cartItems } });
                },
                [cartId]
            ),
            addPhysicalAndVirtualProductItems: useCallback(
                async (simpleCartItems, virtualCartItems) => {
                    await addPhysicalAndVirtualProductItems({
                        variables: { cartId, simpleCartItems, virtualCartItems }
                    });
                },
                [cartId]
            )
        }
    ];
};

export default useAddToCart;
