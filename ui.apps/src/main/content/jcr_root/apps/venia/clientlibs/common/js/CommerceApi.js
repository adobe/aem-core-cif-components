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

    class CommerceApi {

        endpoints = {
            guestcarts: {
                create: '/guest-carts',
                getById: (id) => (`/guest-carts/${id}`)
            }

        };

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

        getCategories() {

        }

        async getCart(id) {
            const url = `${this.rootEndpoint}${this.endpoints.guestcarts.getById(id)}`;
            const cartData = await fetch(url).then(response => response.json());
            return cartData;
        }

        async createCart() {
            const url = `${this.rootEndpoint}${this.endpoints.guestcarts.create}`;

            const response = await fetch(url, {method: "POST"});
            const cartId = await response.json();
            console.log(cartId);
            return cartId;

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