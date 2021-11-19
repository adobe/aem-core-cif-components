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
import { useEventListener } from '../../utils/hooks';
import useAddToCart from './useAddToCart';

const productMapper = item => ({
    data: {
        sku: item.sku,
        quantity: parseFloat(item.quantity)
    }
});

const bundledProductMapper = item => ({
    ...productMapper(item),
    bundle_options: item.options
});

const useAddToCartEvent = (props = {}) => {
    const { fallbackHandler } = props;
    const [, defaultAddToCartApi] = useAddToCart();
    const { addToCartApi = defaultAddToCartApi } = props;

    useEventListener(document, 'aem.cif.add-to-cart', async event => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;
        const physicalCartItems = items.filter(item => !item.virtual).map(productMapper);
        const virtualCartItems = items.filter(item => item.virtual).map(productMapper);
        const bundleCartItems = items.filter(item => item.bundle).map(bundledProductMapper);

        if (bundleCartItems.length > 0) {
            await addToCartApi.addBundledProductItems(bundleCartItems);
        } else if (virtualCartItems.length > 0 && physicalCartItems.length > 0) {
            await addToCartApi.addPhysicalAndVirtualProductItems(physicalCartItems, virtualCartItems);
        } else if (virtualCartItems.length > 0) {
            await addToCartApi.addVirtualProductItems(virtualCartItems);
        } else if (physicalCartItems.length > 0) {
            await addToCartApi.addPhysicalProductItems(physicalCartItems);
        } else if (fallbackHandler) {
            await fallbackHandler(event);
        }
    });
};

export default useAddToCartEvent;
