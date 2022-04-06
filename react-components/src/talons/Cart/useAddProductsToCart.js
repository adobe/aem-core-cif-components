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
import { useCallback } from 'react';
import { useMutation } from '@apollo/client';
import mergeOperations from '@magento/peregrine/lib/util/shallowMerge';
import { useCartContext } from '@magento/peregrine/lib/context/cart';

import MUTATION_ADD_ITEMS_TO_CART from '../../queries/mutation_add_items_to_cart.graphql';

const defaultOperations = {
    addProductItemsMutation: MUTATION_ADD_ITEMS_TO_CART
};

const useAddProductsToCart = (props = {}) => {
    const operations = mergeOperations(defaultOperations, props.operations || {});
    const [addProductItems] = useMutation(operations.addProductItemsMutation);
    const [{ cartId }] = useCartContext();

    return [
        // data
        {
            cartId
        },
        // api
        {
            addProductItems: useCallback(
                async cartItems => {
                    await addProductItems({ variables: { cartId, cartItems } });
                },
                [cartId]
            )
        }
    ];
};

export default useAddProductsToCart;
