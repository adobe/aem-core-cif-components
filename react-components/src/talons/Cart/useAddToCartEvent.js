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
import { useEventListener, useStorefrontEvents } from '../../utils/hooks';
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

const giftCardProductMapper = item => ({
    ...productMapper(item).data,
    entered_options: item.entered_options,
    selected_options: item.selected_options
});

const useAddToCartEvent = (props = {}) => {
    const { fallbackHandler } = props;
    const [{ cartId }, defaultAddToCartApi] = useAddToCart();
    const { addToCartApi = defaultAddToCartApi } = props;
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const mse = typeof props.mse !== 'undefined' ? props.mse : useStorefrontEvents();

    useEventListener(document, 'aem.cif.add-to-cart', async event => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;

        // Since the hook is backwards compatible, we need to determine which items can be added to cart with the latest mutation and map the properties to match the mutation input
        const useUidItems = items
            .filter(item => item.useUid)
            .map(item => ({
                sku: item.parentSku,
                quantity: parseFloat(item.quantity),
                entered_options: item.entered_options,
                selected_options: item.selected_options
            }));
        const nonUidItems = items.filter(item => !item.useUid);
        const physicalCartItems = nonUidItems.filter(item => !item.virtual).map(productMapper);
        const virtualCartItems = nonUidItems.filter(item => item.virtual).map(productMapper);
        const bundleCartItems = nonUidItems.filter(item => item.bundle).map(bundledProductMapper);
        const giftCardCartItems = nonUidItems.filter(item => item.giftCard).map(giftCardProductMapper);

        if (useUidItems.length > 0) {
            await addToCartApi.addProductsToCart(useUidItems);
        }

        if (bundleCartItems.length > 0) {
            await addToCartApi.addBundledProductItems(bundleCartItems);
        } else if (giftCardCartItems.length > 0) {
            await addToCartApi.addGiftCardProductItems(giftCardCartItems);
        } else if (virtualCartItems.length > 0 && physicalCartItems.length > 0) {
            await addToCartApi.addPhysicalAndVirtualProductItems(physicalCartItems, virtualCartItems);
        } else if (virtualCartItems.length > 0) {
            await addToCartApi.addVirtualProductItems(virtualCartItems);
        } else if (physicalCartItems.length > 0) {
            await addToCartApi.addPhysicalProductItems(physicalCartItems);
        } else if (fallbackHandler) {
            await fallbackHandler(event);
        }

        if (mse) {
            const cartItemContext = {
                id: cartId,
                prices: {
                    subtotalExcludingTax: {
                        value: 0,
                        currency: ''
                    }
                },
                items: [],
                possibleOnepageCheckout: false,
                giftMessageSelected: false,
                giftWrappingSelected: false
            };

            items.forEach(item => {
                const { storefrontData } = item;
                const { sku, parentSku, quantity } = item;

                if (!storefrontData || !quantity || (!sku && !parentSku)) {
                    // make sure that all places that do add to cart provide the data
                    // otherwise the integration will not work correctly
                    return;
                }

                const regularPrice = storefrontData.regularPrice || 0;
                const specialPrice = storefrontData.finalPrice || 0;
                const currency = storefrontData.currencyCode || '';
                const name = storefrontData.name || sku;
                const options = storefrontData.selectedOptions || [];

                cartItemContext.items.push({
                    quantity,
                    product: { name, sku: parentSku || sku, pricing: { regularPrice, specialPrice } },
                    prices: { price: { value: specialPrice, currency } },
                    configurableOptions: options.map(o => ({ optionLabel: o.attribute, valueLabel: o.value }))
                });
                cartItemContext.prices.subtotalExcludingTax.value += specialPrice * quantity;
                cartItemContext.prices.subtotalExcludingTax.currency = currency;
            });

            mse.context.setShoppingCart(cartItemContext);
            mse.publish.addToCart();
        }
    });
};

export default useAddToCartEvent;
