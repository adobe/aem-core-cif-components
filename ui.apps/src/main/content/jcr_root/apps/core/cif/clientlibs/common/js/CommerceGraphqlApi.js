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
        if (!props || !props.endpoint) {
            throw new Error(
                'The commerce API is not properly initialized. The "endpoint" property is missing from the initialization object'
            );
        }

        this.endpoint = props.endpoint;
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
                'Content-Type': 'application/json'
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

        // prettier-ignore
        const variantQuery = `... on ConfigurableProduct {
            variants {
                product {
                    sku
                    price {
                        regularPrice {
                            amount {
                                currency
                                value
                            }
                        }
                    }
                }
            }
        }`;

        // prettier-ignore
        const query = `query {
            products(filter: { sku: { in: [${skuQuery}] }} ) {
                items {
                    sku
                    price {
                        regularPrice {
                            amount {
                                currency
                                value
                            }
                        }
                    }
                    ${includeVariants ? variantQuery : ''}
                }
            }
        }`;
        let response = await this._fetchGraphql(query);

        // Transform response in a SKU to price map
        let items = response.data.products.items;
        let dict = {};
        for (let item of items) {
            dict[item.sku] = item.price.regularPrice.amount;

            // Go through variants
            if (!item.variants) continue;
            for (let variant of item.variants) {
                dict[variant.product.sku] = variant.product.price.regularPrice.amount;
            }
        }
        return dict;
    }
}

(function() {
    function onDocumentReady() {
        const endpoint = '/magento/graphql';
        window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({ endpoint });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export default CommerceGraphqlApi;
