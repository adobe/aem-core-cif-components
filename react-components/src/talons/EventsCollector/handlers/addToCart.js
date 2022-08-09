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
const canHandle = event => event.type === 'CART_ADD_ITEM';

const handle = (sdk, event) => {
    const { payload } = event;

    const { cartId, currencyCode, priceTotal, quantity, name, sku, selectedOptions } = payload;

    const configurableOptions = selectedOptions
        ? [
              {
                  optionLabel: selectedOptions.attribute,
                  valueLabel: selectedOptions.value
              }
          ]
        : null;

    const cartItemContext = {
        id: cartId,
        prices: {
            subtotalExcludingTax: {
                value: priceTotal * quantity,
                currency: currencyCode
            }
        },
        items: [
            {
                product: {
                    name: name,
                    sku: sku,
                    configurableOptions: configurableOptions
                },
                prices: {
                    price: {
                        value: priceTotal,
                        currency: currencyCode
                    }
                }
            }
        ],
        possibleOnepageCheckout: false,
        giftMessageSelected: false,
        giftWrappingSelected: false
    };

    sdk.context.setShoppingCart(cartItemContext);
    sdk.publish.addToCart();
};

export default {
    canHandle,
    handle
};
