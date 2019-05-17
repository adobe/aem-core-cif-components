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

/**
 * The CommerceApi is responsible for interacting with the Commerce backend using REST API calls
 */
(function () {

    'use strict';
    const endpoints = {
        guestcarts: {
            create: '/guest-carts',
            byId: (id) => (`/guest-carts/${id}`),
            addEntry: (id) => (`/guest-carts/${id}/items`),
            totals: (id) => (`/guest-carts/${id}/totals`),
            itemOperation: (cartId, itemId) => (`/guest-carts/${cartId}/items/${itemId}`)
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

        /**
         * Issues a request to the supplied URL using the provided parameters
         * @param url
         * @param params
         * @returns {Promise<any>} the JSON response or throws an error
         * @private
         */
        async _fetch(url, params) {
            let response = await fetch(url, params);
            if (!response.ok) {
                let message = await response.text();
                throw new Error(message);
            }
            return response.json();
        }

        /**
         * Performs an update operation (POST or PUT).
         * @param url the request URL
         * @param params the URL parameters
         * @param method the method to use for the update - POST or PUT
         * @returns {Promise<any>}
         * @private
         */
        async _update(endpoint, params, method) {
            let url = `${this.rootEndpoint}${endpoint}`;
            let defaultParams = {
                method,
                headers: {
                    'Content-Type': 'application/json'
                },
            };

            let extendedParams = Object.assign({}, params, defaultParams);

            return this._fetch(url, extendedParams);
        }

        async _post(endpoint, params) {
            return this._update(endpoint, params, 'POST');
        }

        async _put(endpoint, params) {
            return this._update(endpoint, params, 'PUT');
        }

        async _get(endpoint) {
            let url = `${this.rootEndpoint}${endpoint}`;
            return this._fetch(url);
        }

        async _delete(endpoint) {
            let url = `${this.rootEndpoint}${endpoint}`;
            let params = {"method": "DELETE"};

            return this._fetch(url, params);
        }

        /**
         * Retrieves the cart data in JSON format
         * @param id the cart id
         * @returns {Promise<T>}
         */
        async getCart(id) {
            return await this._get(endpoints.guestcarts.byId(id));
        }

        /**
         * Creates an empty shopping cart.
         * @returns {Promise<*>}
         */
        async createCart() {
            return await this._post(endpoints.guestcarts.create);
        }

        /**
         * Updates a cart entry
         * @param cartId
         * @param itemId
         * @param sku
         * @param qty
         * @param quoteId
         * @returns {Promise<*>}
         */
        async updateCartEntry(cartId, itemId, {sku, qty, quoteId}) {
            let url = `${endpoints.guestcarts.itemOperation(cartId, itemId)}`;
            let params = {
                cartItem: {
                    sku,
                    qty,
                    quote_id: quoteId
                }
            };

            let body = {body: JSON.stringify(params)};
            return this._put(url, body)

        }

        /**
         * Adds a new cart entry
         * @param cartId
         * @param sku
         * @param qty
         * @param quoteId
         * @returns {Promise<*>}
         */
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

        /**
         * Retrieves the cart totals.
         * @param cartId
         * @returns {Promise<T>}
         */
        async getTotals(cartId) {
            return await this._get(endpoints.guestcarts.totals(cartId));
        }

        /**
         * Removes an item from the cart
         * @param cartQuote
         * @param itemId
         * @returns {Promise<*>}
         */
        async removeItem(cartQuote, itemId) {
            return await this._delete(endpoints.guestcarts.itemOperation(cartQuote, itemId));
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