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

export const addConfigurableProductEvent = {
    type: 'CART_ADD_ITEM',
    payload: {
        cartId: 'kAR5Gg6uPC6J5wGY0ebecyKfX905epmU',
        sku: 'VSK03',
        name: 'Johanna Skirt',
        priceTotal: 78,
        currencyCode: 'USD',
        discountAmount: 0,
        selectedOptions: [
            {
                attribute: 'Fashion Color',
                value: 'Peach'
            },
            {
                attribute: 'Fashion Size',
                value: 'M'
            }
        ],
        quantity: 1
    }
};

export const addSimpleProductEvent = {
    type: 'CART_ADD_ITEM',
    payload: {
        cartId: 'kAR5Gg6uPC6J5wGY0ebecyKfX905epmU',
        sku: 'VA15-SI-NA',
        name: 'Silver Sol Earrings',
        priceTotal: 48,
        currencyCode: 'USD',
        discountAmount: 0,
        selectedOptions: [],
        quantity: 1
    }
};
