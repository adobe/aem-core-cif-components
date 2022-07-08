/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
let dataLayerEnabled = null;
let dataLayer = null;

export const isDataLayerEnabled = () => {
    if (dataLayerEnabled === null) {
        dataLayerEnabled = document.body.hasAttribute('data-cmp-data-layer-enabled');
    }
    return dataLayerEnabled;
};
const getDataLayer = () => {
    if (dataLayer === null) {
        dataLayer = isDataLayerEnabled() ? (window.adobeDataLayer = window.adobeDataLayer || []) : undefined;
    }
    return dataLayer;
};

export const generateDataLayerId = async (prefix, idData, separator = '-') => {
    const msgUint8 = new TextEncoder().encode(idData); // encode as (utf-8) Uint8Array
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8); // hash the message
    const hashArray = Array.from(new Uint8Array(hashBuffer)); // convert buffer to byte array
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join(''); // convert bytes to hex string
    return prefix + separator + hashHex.substring(0, 10);
};

// https://github.com/adobe/adobe-client-data-layer/wiki#push
export const pushData = data => {
    if (isDataLayerEnabled()) {
        getDataLayer().push(data);
    }
};

// https://github.com/adobe/adobe-client-data-layer/wiki#push
export const pushEvent = (eventName, eventInfo, extraData) => {
    if (isDataLayerEnabled()) {
        getDataLayer().push({
            event: eventName,
            eventInfo: eventInfo,
            ...extraData
        });
    }
};

// https://github.com/adobe/adobe-client-data-layer/wiki#getstate
export const getState = reference => {
    if (isDataLayerEnabled()) {
        return getDataLayer().getState(reference);
    }
    return null;
};

// https://github.com/adobe/adobe-client-data-layer/wiki#addeventlistener
export const addEventListener = (eventName, handler) => {
    if (isDataLayerEnabled()) {
        getDataLayer().addEventListener(eventName, handler);
    }
};

// https://github.com/adobe/adobe-client-data-layer/wiki#removeeventlistener
export const removeEventListener = (eventName, handler) => {
    if (isDataLayerEnabled()) {
        getDataLayer().removeEventListener(eventName, handler);
    }
};

/**
 * Converts all keys of a given GraphQL response from kebab-case or snake_case into camelCase.
 */
const transformGraphqlResponse = o => {
    const result = {};

    Object.entries(o).forEach(([key, value]) => {
        if (key === '__typename') {
            return;
        }

        if (Array.isArray(value)) {
            result[toCamel(key)] = value.map(item => transformGraphqlResponse(item));
        } else if (typeof value === 'object' && value !== null) {
            result[toCamel(key)] = transformGraphqlResponse(value);
        } else {
            result[toCamel(key)] = value;
        }
    });
    return result;
};

const toCamel = s =>
    s.replace(/([-_][a-z])/gi, group =>
        group
            .toUpperCase()
            .replace('-', '')
            .replace('_', '')
    );

/**
 * Remove fields which are not required by MSE SDK and add fields which are unavailable in GraphQL.
 */
export const transformCart = cart => {
    let newCart = {};
    try {
        const { id, prices, totalQuantity } = cart;
        const { subtotalExcludingTax, subtotalIncludingTax } = prices || {};

        const items = cart.items
            ? cart.items.map(item => {
                  const { price } = item.prices;
                  const { value, currency } = price;
                  return {
                      prices: {
                          price
                      },
                      canApplyMsrp: false,
                      id: item.uid,
                      formattedPrice: `${value} ${currency}`,
                      quantity: item.quantity,
                      product: {
                          productId: 0,
                          name: item.product.name,
                          sku: item.product.sku
                      }
                  };
              })
            : [];

        newCart = {
            id,
            items,
            prices: {
                subtotalExcludingTax,
                subtotalIncludingTax
            },
            totalQuantity
        };
    } catch (e) {
        console.error(e);
    }

    return newCart;
};

export { transformGraphqlResponse };

export default {
    isEnabled: isDataLayerEnabled,
    getState,
    pushEvent,
    pushData,
    generateId: generateDataLayerId
}