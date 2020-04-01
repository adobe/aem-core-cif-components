/*******************************************************************************
 *
 *     Copyright 2019 Adobe. All rights reserved.
 *     This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License. You may obtain a copy
 *     of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under
 *     the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *     OF ANY KIND, either express or implied. See the License for the specific language
 *     governing permissions and limitations under the License.
 *
 ******************************************************************************/
'use strict';

class CommerceGraphqlApi {
    constructor(props) {
        if (!props || !props.endpoint || !props.storeView) {
            throw new Error(
                'The commerce API is not properly initialized. A required property is missing from the initialization object'
            );
        }

        this.endpoint = props.endpoint;
        this.storeView = props.storeView;
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
            method: ignoreCache ? 'POST' : 'GET',
            headers: {
                'Content-Type': 'application/json',
                Store: this.storeView
            }
        };

        let url = this.endpoint;
        if (ignoreCache) {
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
     * Retrieves the URL of the images for an array of product data
     * @param productData a dictionary object with the following structure {productName:productSku}.
     * The productName is used for filtering the query and the product SKU is used to identify the variant for which to retrieve the image
     * @returns {Promise<any[]>}
     */
    async getProductImageUrls(productData) {
        //ugly but effective
        let names = Object.keys(productData).reduce((acc, name) => (acc += '"' + name + '",'), '');
        if (names.length === 0) {
            return {};
        }
        // prettier-ignore
        const query = `query { 
            products(filter: {name: {in: [${names.substring(0, names.length - 1)}]}}) {
                items {
                    sku
                    name
                    thumbnail {  
                        url
                        }
                    ... on ConfigurableProduct {
                        variants {
                            product {
                                sku
                                thumbnail {  
                                   url
                                }
                            }
                        }
                    }
                }
            }
        }`;

        let response = await this._fetchGraphql(query);
        let items = response.data.products.items;

        let productsMedia = {};
        items.forEach(item => {
            let variants = item.variants;
            if (variants && variants.length > 0) {
                let skus = productData[item.name];
                let media = variants.filter(v => skus.indexOf(v.product.sku) !== -1);
                if (media && media.length > 0) {
                    media.forEach(v => {
                        productsMedia[v.product.sku] = v.product.thumbnail.url;
                    });
                }
            } else {
                productsMedia[item.sku] = item.thumbnail.url;
            }
        });
        return productsMedia;
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

(function() {
    function onDocumentReady() {
        const { storeView, graphqlEndpoint } = document.querySelector('body').dataset;
        window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({ endpoint: graphqlEndpoint, storeView });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export default CommerceGraphqlApi;
