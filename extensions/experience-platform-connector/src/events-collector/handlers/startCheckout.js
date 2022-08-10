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
import { getCartTotal, getCurrency, getFormattedProducts } from '../utils';

const canHandle = event => event.type === 'CHECKOUT_PAGE_VIEW';

const handle = (sdk, event) => {
    const { payload } = event;

    const { cart_id, products } = payload;

    // Send out page view event
    const pageContext = {
        pageType: 'Checkout',
        pageName: 'Checkout',
        eventType: 'visibilityHidden',
        maxXOffset: 0,
        maxYOffset: 0,
        minXOffset: 0,
        minYOffset: 0
    };

    sdk.context.setPage(pageContext);
    sdk.publish.pageView();

    const cartContext = {
        id: cart_id,
        prices: {
            subtotalExcludingTax: {
                value: getCartTotal(products),
                currency: getCurrency(products)
            }
        },
        items: getFormattedProducts(products),
        possibleOnepageCheckout: false,
        giftMessageSelected: false,
        giftWrappingSelected: false
    };

    sdk.context.setShoppingCart(cartContext);
    sdk.publish.initiateCheckout();
};

export default {
    canHandle,
    handle
};
