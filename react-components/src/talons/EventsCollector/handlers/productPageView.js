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
const canHandle = event => event.type === 'PRODUCT_PAGE_VIEW';

const handle = (sdk, event) => {
    const { payload } = event;

    const { name, id, currency_code, price_range, sku, url_key } = payload;

    const pageContext = {
        pageType: 'PDP',
        pageName: name,
        eventType: 'visibilityHidden',
        maxXOffset: 0,
        maxYOffset: 0,
        minXOffset: 0,
        minYOffset: 0
    };

    sdk.context.setPage(pageContext);

    sdk.publish.pageView();

    const productContext = {
        productId: id,
        name,
        sku,
        pricing: {
            currencyCode: currency_code,
            maximalPrice: price_range.maximum_price.final_price
        },
        canonicalUrl: url_key
    };

    sdk.context.setProduct(productContext);

    sdk.publish.productPageView();
};

export default {
    canHandle,
    handle
};
