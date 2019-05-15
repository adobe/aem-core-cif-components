/*******************************************************************************
 *
 *      Copyright 2019 Adobe. All rights reserved.
 *      This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License. You may obtain a copy
 *      of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software distributed under
 *      the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *      OF ANY KIND, either express or implied. See the License for the specific language
 *      governing permissions and limitations under the License.
 *
 *
 ******************************************************************************/

window.CIF = window.CIF || {};

(function () {

    const endpoints = {
        guestcarts: {
            create: '/guest-carts',
            byId: (id) => (`/guest-carts/${id}`),
            addEntry: (id) => (`/guest-carts/${id}/items`),
            totals: (id) => (`/guest-carts/${id}/totals`),
            removeItem: (cartId,itemId) => (`/guest-carts/${cartId}/items/${itemId}`)
        }

    };

    class CommerceApi {

        /**
         * initializes the commerce API.
         * @param props {Object} the props have the following structure: { endpoint }
         */
        constructor(props) {
            if (!props.endpoint) {
                throw new Error('The commerce API is not properly initialized. The "endpoint" property is missing from the initialization object');
            }

            this.rootEndpoint = props.endpoint;
        }

        async _fetch(url, params) {
            let response = await fetch(url, params);
            if (!response.ok) {
                let message = await response.text();
                throw new Error(message);
            }
            return response.json();
        }

        async _post(endpoint, params) {

            let url = `${this.rootEndpoint}${endpoint}`;
            let defaultParams = {
                method: "POST",
                headers: {
                    'Content-Type': 'application/json'
                },
            };

            let extendedParams = Object.assign({}, params, defaultParams);

            return this._fetch(url,extendedParams);
        }

        //TODO update error checking
        async _get(endpoint) {
            let url = `${this.rootEndpoint}${endpoint}`;
            return fetch(url).catch(err => {
                throw new Error(err);
            })
        }

        async _delete(endpoint) {
            let url = `${this.rootEndpoint}${endpoint}`;
            let params = {"method": "DELETE"};

            return this._fetch(url, params);
        }

        async getCart(id) {
            return await this._get(endpoints.guestcarts.byId(id)).then(response => response.json());
        }

        async createCart() {

            return await this._post(endpoints.guestcarts.create);
        }

        async postCartEntry(cartId, {sku, qty, quoteId}) {
            const url = `${endpoints.guestcarts.addEntry(cartId)}`;
            const params = {
                cartItem: {
                    sku,
                    qty,
                    quote_id: quoteId
                }
            };
            const body = {body: JSON.stringify(params)};
            return await this._post(url, body);
        }

        async getTotals(cartId) {
            return await this._get(endpoints.guestcarts.totals(cartId)).then(response => response.json());
        }

        async removeItem(cartQuote, itemId) {
            return await this._delete(endpoints.guestcarts.removeItem(cartQuote, itemId));
        }


    }

    function onDocumentReady() {
        const endpoint = "http://localhost/magento/rest/default/V1";

        window.CIF.CommerceApi = new CommerceApi({endpoint});
    }


    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

}());