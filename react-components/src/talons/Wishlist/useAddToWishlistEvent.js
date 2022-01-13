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
import { useEventListener } from '../../utils/hooks';
import defaultOperations from '@magento/peregrine/lib/talons/Wishlist/AddToListButton/addToListButton.gql';
import mergeOperations from '@magento/peregrine/lib/util/shallowMerge';
import { useMutation } from '@apollo/client';

const productMapper = item => {
    const result = {
        sku: item.sku,
        quantity: item.quantity
    };

    if (item.parent_sku) {
        result.parent_sku = item.parent_sku;
    }

    return result;
};

const useAddToWishlistEvent = (props = {}) => {
    const operations = mergeOperations(defaultOperations, props.operations);
    const [addProductToWishlist] = useMutation(operations.addProductToWishlistMutation);

    useEventListener(document, 'aem.cif.add-to-wishlist', async event => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;

        const promises = items.map(item =>
            addProductToWishlist({ variables: { wishlistId: '0', itemOptions: productMapper(item) } })
        );

        let toastEvent;
        try {
            // Wait for all items to be added to the wishlist
            await Promise.all(promises);
            toastEvent = new CustomEvent('aem.cif.toast', {
                detail: {
                    message: 'wishlist.success',
                    type: 'info'
                }
            });
        } catch (error) {
            toastEvent = new CustomEvent('aem.cif.toast', {
                detail: {
                    message: 'wishlist.error',
                    type: 'error',
                    error
                }
            });
        }
        document.dispatchEvent(toastEvent);
    });
};

export default useAddToWishlistEvent;
