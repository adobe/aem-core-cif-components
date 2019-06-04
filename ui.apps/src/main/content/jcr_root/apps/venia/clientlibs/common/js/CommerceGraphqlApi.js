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


(function () {

    'use strict';
    //todo update the proxy rules so that this gets router through the dispatcher
    const imageUrlPrefix = "/magento/img";

    class CommerceGraphqlApi {

        constructor(props) {
            if (!props.endpoint) {
                throw new Error('The commerce API is not properly initialized. The "endpoint" property is missing from the initialization object');
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

        /**
         * Retrieves the URL of the images for an array of product data
         * @param productData a dictionary object with the following structure {productName:productSku}.
         * The productName is used for filtering the query and the product SKU is used to identify the variant for which to retrieve the image
         * @returns {Promise<any[]>}
         */
        async getProductImageUrls(productData) {
            //ugly but effective
            let names = Object.keys(productData).reduce((acc, name) => (acc += '\"' + name + '\",'), '');
            if (names.length === 0) {
                return {};
            }
            const query = `query { products(filter: {name: {in: [${names.substring(0, names.length - 1)}]}}) { items { sku name ... on ConfigurableProduct { variants { product { sku media_gallery_entries { file } } } } } } }`;
            console.log(query);

            let params = {
                method: "POST",
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({query})
            };

            let response = await this._fetch(this.endpoint, params);
            if (response.errors) {
                throw new Error(JSON.stringify(response.errors));
            }
            let items = response.data.products.items;

            let productsMedia = {};

            items.forEach(item => {
                let variants = item.variants;
                if (variants.length > 0) {

                    let skus = productData[item.name];
                    let media = variants.filter(v => skus.indexOf(v.product.sku) !== -1);
                    if (media && media.length > 1) {
                        media.forEach( v => productsMedia[v.product.sku] = `${imageUrlPrefix}${v.product.media_gallery_entries[0].file}`);
                    }
                }
            });
            return productsMedia;
        }
    }

    function onDocumentReady() {
        const endpoint = "/magento/graphql";

        window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({endpoint});
    }


    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})();