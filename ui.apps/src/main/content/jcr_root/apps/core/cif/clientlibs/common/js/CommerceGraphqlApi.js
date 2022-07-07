/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

class CommerceGraphqlApi {
    constructor(props) {
        if (!props || !props.graphqlEndpoint) {
            throw new Error(
                'The commerce API is not properly initialized. A required property is missing from the initialization object'
            );
        }

        this.endpoint = props.graphqlEndpoint;
        this.storeView = props.storeView;
        this.method = props.graphqlMethod;
        this.headers = props.headers;
    }

    async _fetch(url, params) {
        let response = await fetch(url, params);
        if (!response.ok || response.errors) {
            let message = await response.text();
            throw new Error(message);
        }
        return response.json();
    }

    async _fetchGraphql(query, ignoreCache = false) {
        // Minimize query
        query = query
            .split('\n')
            .map(a => a.trim())
            .join(' ');
        query = { query };

        let params = {
            method: this.method === 'GET' && !ignoreCache ? 'GET' : 'POST',
            headers: {
                ...this.headers,
                'Content-Type': 'application/json'
            }
        };

        if (this.storeView) {
            params.headers['Store'] = this.storeView;
        }

        const loginToken = this._getLoginToken();
        if (loginToken) {
            params.headers['Authorization'] = `Bearer ${loginToken}`;
        }

        let url = this.endpoint;
        if (params.method === 'POST') {
            // For un-cached POST request, add query to body
            params.body = JSON.stringify(query);
        } else {
            // For cached GET request, add query as query parameters
            let queryString = Object.keys(query)
                .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(query[k])}`)
                .join('&');
            url += '?' + queryString;
        }

        let response = await this._fetch(url, params);
        if (response.data === undefined && response.errors) {
            throw new Error(JSON.stringify(response.errors));
        }

        return response;
    }

    /**
     * Retrieves the prices of the products with the given SKUs and their variants using getProductPrices() and coverts
     * them using the PriceToPriceRangeConverter.
     *
     * @param {array} skus  Array of product SKUs.
     * @returns {Promise<any[]>} Returns a map of skus mapped to their prices. The price is an object containing the
     *                           currency and value.
     */
    async getProductPriceModels(skus, includeVariants) {
        const prices = await this.getProductPrices(skus, includeVariants);
        const priceRanges = {};

        for (let key in prices) {
            priceRanges[key] = window.CIF.PriceToPriceModelConverter(prices[key]);
        }

        return priceRanges;
    }

    /**
     * Retrieves the login token from local storage as it is stored by Peregrine.
     *
     * @returns {string} login token or null if not logged in.
     */
    _getLoginToken() {
        const key = 'M2_VENIA_BROWSER_PERSISTENCE__signin_token';
        let token = this._checkCookie('cif.userToken') ? this._cookieValue('cif.userToken') : '';

        try {
            let lsToken = JSON.parse(localStorage.getItem(key));
            if (lsToken && lsToken.value) {
                const timestamp = new Date().getTime();
                if (timestamp - lsToken.timeStored < lsToken.ttl * 1000) {
                    token = lsToken.value.replace(/"/g, '');
                }
            }
        } catch (e) {
            console.error(`Login token at ${key} is not valid JSON.`);
        }

        return token;
    }

    /**
     * Checks if a cookie with the given name exists.
     *
     * @param {string} cookieName
     * @returns {boolean} true if the cookie exists, false otherwise.
     */
    _checkCookie(cookieName) {
        return document.cookie.split(';').filter(item => item.trim().startsWith(`${cookieName}=`)).length > 0;
    }

    /**
     * Returns the value of the cookie with the given name.
     *
     * @param {string} cookieName
     * @returns {string} value of the cookie or empty string if the cookie does not exist.
     */
    _cookieValue(cookieName) {
        let b = document.cookie.match(`(^|[^;]+)\\s*${cookieName}\\s*=\\s*([^;]+)`);
        return b ? b.pop() : '';
    }

    /**
     * When called requests to getProductPrices() will defer the query execution until resumeGetProductPrices() is
     * called. The promises returned will only resolve after that.
     */
    /* private */ _suspendGetProductPrices() {
        // by defining the queue, all invocations to getProductPrices will add an entry there instead of executing the
        // query the queue is deleted in _resumeGetProductPrices to restore the original behaviour.
        this.getProductPricesQueue = [];
    }

    /* private */ _resumeGetProductPrices() {
        if (!this.getProductPricesQueue) {
            return;
        }

        const filterPrices = (response, skus, includeVariants) => {
            const returns = skus.reduce((result, sku) => {
                let prices = response[sku];
                if (!includeVariants) {
                    // only return the requested sku
                    result[sku] = prices[sku];
                } else {
                    // include all prices
                    for (let key of Object.keys(prices)) {
                        result[key] = prices[key];
                    }
                }
                return result;
            }, {});
            return returns;
        };

        let queue = this.getProductPricesQueue;
        // remove the queue to restore the behaviour of getProductPrices()
        this.getProductPricesQueue = null;

        let callsWithVariants = queue.filter(entry => entry.skus && entry.includeVariants);
        let allSkusWithVariants = callsWithVariants
            .reduce((skus, entry) => entry.skus.forEach(sku => skus.push(sku)) || skus, [])
            .filter((value, index, array) => array.indexOf(value) === index);
        let fetchWithVariants$ = this._doFetchProductPrices(allSkusWithVariants, true);
        fetchWithVariants$
            .then(resp =>
                callsWithVariants.forEach(entry => entry.resolve(filterPrices(resp, entry.skus, entry.includeVariants)))
            )
            .catch(err => callsWithVariants.forEach(entry => entry.reject(err)));

        let callsWithoutVariants = queue.filter(entry => entry.skus && !entry.includeVariants);
        let allSkusWithoutVariants = callsWithoutVariants
            .reduce((skus, entry) => entry.skus.forEach(sku => skus.push(sku)) || skus, [])
            .filter((value, index, array) => array.indexOf(value) === index)
            // filter out skus that were already requested in the with variants query
            .filter(sku => allSkusWithVariants.indexOf(sku) < 0);

        Promise.all([fetchWithVariants$, this._doFetchProductPrices(allSkusWithoutVariants, false)])
            .then(([pricesWithVariants, pricesWithoutVariants]) => ({
                ...pricesWithVariants,
                ...pricesWithoutVariants
            }))
            .then(resp =>
                callsWithoutVariants.forEach(entry =>
                    entry.resolve(filterPrices(resp, entry.skus, entry.includeVariants))
                )
            )
            .catch(err => callsWithoutVariants.forEach(entry => entry.reject(err)));
    }

    /**
     * Retrieves the prices of the products with the given SKUs and their variants.
     *
     * @param {array} skus  Array of product SKUs.
     * @returns {Promise<any[]>} Returns a map of skus mapped to their prices. The price is an object containing the
     *                           currency and value.
     */
    async getProductPrices(skus, includeVariants) {
        if (this.getProductPricesQueue) {
            return new Promise((resolve, reject) => {
                this.getProductPricesQueue.push({ skus, includeVariants, resolve, reject });
            });
        } else {
            let prices = await this._doFetchProductPrices(skus, includeVariants);
            return Object.values(prices).reduce((result, map) => ({ ...result, ...map }), {});
        }
    }

    /* private */ async _doFetchProductPrices(skus, includeVariants) {
        if (!skus || skus.length === 0) {
            // don't query if no skus are given
            return {};
        }

        // distinct skus
        skus = skus.filter((v, i, a) => a.indexOf(v) === i);

        let skuQuery = '"' + skus.join('", "') + '"';

        const priceQuery = `regular_price {
                value
                currency
            }
            final_price {
                value
                currency
            }
            discount {
                amount_off
                percent_off
            }`;

        // prettier-ignore
        const variantQuery = `variants {
            product {
                sku
                price_range {
                    minimum_price {${priceQuery}}
                }
            }
        }`;

        // prettier-ignore
        const query = `query {
            products(filter: { sku: { in: [${skuQuery}] }} ) {
                items {
                    __typename
                    sku
                    price_range {
                        minimum_price {${priceQuery}}
                    }
                    ... on ConfigurableProduct {
                        price_range {
                            maximum_price {${priceQuery}}
                        }
                        ${includeVariants ? variantQuery : ''}
                    }
                    ... on GroupedProduct {
                        items {
                            product {
                                sku
                                price_range {
                                    minimum_price {${priceQuery}}
                                }
                            }
                        }
                    }
                    ... on BundleProduct {
                        price_range {
                            maximum_price {${priceQuery}}
                        }
                    }
                }
            }
        }`;

        let response = await this._fetchGraphql(query);

        // Transform response in a SKU to price map and put it in the cache
        let items = response.data.products.items;
        let result = {};

        for (let item of items) {
            let dict = {};
            result[item.sku] = dict;
            dict[item.sku] = { __typename: item.__typename, ...item.price_range };

            // Go through variants
            if (item.variants) {
                for (let variant of item.variants) {
                    dict[variant.product.sku] = { __typename: item.__typename, ...variant.product.price_range };
                }
            }
            // Go through grouped products
            if (item.__typename == 'GroupedProduct' && item.items) {
                for (let variant of item.items) {
                    dict[variant.product.sku] = { __typename: item.__typename, ...variant.product.price_range };
                }
            }
        }

        return result;
    }
}

export default CommerceGraphqlApi;
