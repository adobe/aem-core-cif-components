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
            addEntry: (id) => (`/guest-carts/${id}/items`)
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

        async _post(url, params) {

            let defaultParams = {
                method: "POST"
            };

            let extendedParams = Object.assign({}, params, defaultParams);
            return fetch(url, extendedParams).catch(err => {
                throw new Error(err);
            })
        }

        async getCart(id) {
            const url = `${this.rootEndpoint}${endpoints.guestcarts.byId(id)}`;
            const cartData = await fetch(url).then(response => response.json());
            return cartData;
        }

        async createCart() {
            const url = `${this.rootEndpoint}${endpoints.guestcarts.create}`;

            const response = await fetch(url, {method: "POST"});
            const cartId = await response.json();
            return cartId;

        }

        async postCartEntry(cartId, {sku, qty, quoteId}) {
            const url = `${this.rootEndpoint}${endpoints.guestcarts.addEntry(cartId)}`;
            const params = {
                id: sku,
                qty,
                quote_id: quoteId
            };
            const entry = await this._post(url, params);

            return entry;
        }


    }

    function onDocumentReady() {
        const endpoint = "http://localhost/magento/rest/default/V1";

        window.CIF.CommerceApi = new CommerceApi({endpoint});
        createTestCart();
    }

    async function createTestCart() {
        if (window.CIF.PageContext.cartInfo && window.CIF.PageContext.cartInfo.cartId && window.CIF.PageContext.cartInfo.cartQuote) {
            return;
        }
        
        let cartInfo = {};
        let cartQuote = await window.CIF.CommerceApi.createCart();
        let cart = await window.CIF.CommerceApi.getCart(cartQuote);

        cartInfo.cartQuote = cartQuote;
        cartInfo.cartId = cart.id;
        window.CIF.PageContext.setCartInfo(cartInfo);
    }

    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

}());