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
'use strict';

/**
 * Convert given GraphQL PriceRange object into data structure as defined by the sling model.
 */
const PriceToPriceRangeConverter = range => {
    let price = {};
    price.productType = range.__typename;
    price.currency = range.minimum_price.final_price.currency;
    price.regularPrice = range.minimum_price.regular_price.value;
    price.finalPrice = range.minimum_price.final_price.value;

    if (range.minimum_price.discount) {
        price.discountAmount = range.minimum_price.discount.amount_off;
        price.discountPercent = range.minimum_price.discount.percent_off;
    }

    if (range.maximum_price) {
        price.regularPriceMax = range.maximum_price.regular_price.value;
        price.finalPriceMax = range.maximum_price.final_price.value;
        if (range.maximum_price.discount) {
            price.discountAmountMax = range.maximum_price.discount.amount_off;
            price.discountPercentMax = range.maximum_price.discount.percent_off;
        }
    }

    price.discounted = !!(price.discountAmount && price.discountAmount > 0);
    price.range = !!(
        price.finalPrice &&
        price.finalPriceMax &&
        Math.round(price.finalPrice * 100) != Math.round(price.finalPriceMax * 100)
    );

    return price;
};

export default PriceToPriceRangeConverter;
