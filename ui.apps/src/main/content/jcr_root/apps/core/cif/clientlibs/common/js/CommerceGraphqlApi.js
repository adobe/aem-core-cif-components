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

        // Check if authorization token available
        const checkCookie = cookieName => {
            return document.cookie.split(';').filter(item => item.trim().startsWith(`${cookieName}=`)).length > 0;
        };

        const cookieValue = cookieName => {
            let b = document.cookie.match(`(^|[^;]+)\\s*${cookieName}\\s*=\\s*([^;]+)`);
            return b ? b.pop() : '';
        };

        let token = checkCookie('cif.userToken') ? cookieValue('cif.userToken') : '';

        let params = {
            method: this.method === 'GET' && !ignoreCache ? 'GET' : 'POST',
            headers: {
                ...this.headers,
                'Content-Type': 'application/json'
            }
        };

        if (token.length > 0) {
            params.headers['authorization'] = `Bearer ${token && token.length > 0 ? token : ''}`;
        }

        if (this.storeView) {
            params.headers['Store'] = this.storeView;
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
     * Retrieves the prices of the products with the given SKUs and their variants.
     *
     * @param {array} skus  Array of product SKUs.
     * @returns {Promise<any[]>} Returns a map of skus mapped to their prices. The price is an object containing the currency and value.
     */
    async getProductPrices(skus, includeVariants) {
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

        // Transform response in a SKU to price map
        let items = response.data.products.items;
        let dict = {};
        for (let item of items) {
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
        return dict;
    }
}

export default CommerceGraphqlApi;
