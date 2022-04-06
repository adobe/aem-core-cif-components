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
import { useEventListener } from '../../utils/hooks';
import useAddProductsToCart from './useAddProductsToCart';

const useAddProductsToCartEvent = (props = {}) => {
    const [, defaultAddToCartApi] = useAddProductsToCart();
    const { addToCartApi = defaultAddToCartApi } = props;

    useEventListener(document, 'aem.cif.add-to-cart', async event => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;
        const mappedItems = items.map(item => ({
            sku: item.parentSku,
            quantity: parseFloat(item.quantity),
            entered_options: item.entered_options,
            selected_options: item.selected_options
        }));
        await addToCartApi.addProductItems(mappedItems);
    });
};

export default useAddProductsToCartEvent;
