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
import { useEventListener } from './hooks';
import * as dataLayerUtils from './dataLayerUtils';

const useDataLayerEvents = () => {
    useEventListener(document, 'aem.cif.add-to-cart', async event => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;
        items.forEach(item => {
            let eventInfo = {
                '@id': item.productId,
                'xdm:SKU': item.sku,
                'xdm:quantity': item.quantity
            };

            if (item.bundle) {
                eventInfo['bundle'] = true;
            }

            dataLayerUtils.pushEvent('cif:addToCart', eventInfo);
        });
    });

    useEventListener(document, 'aem.cif.add-to-wishlist', async event => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;
        items.forEach(item => {
            let eventInfo = {
                '@id': item.productId,
                'xdm:SKU': item.sku,
                'xdm:quantity': item.quantity
            };

            dataLayerUtils.pushEvent('cif:addToWishList', eventInfo);
        });
    });
};

export default useDataLayerEvents;
