/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import PriceFormatter from './PriceFormatter';
import CommerceGraphqlApi from './CommerceGraphqlApi';

(function() {
    function onDocumentReady() {
        try {
            window.CIF = window.CIF || {};

            // define PriceFormatter
            window.CIF.PriceFormatter = PriceFormatter;

            // initialize CommerceGraphqlApi
            const storeConfigEl = document.querySelector('meta[name="store-config"]');

            if (storeConfigEl) {
                window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi(JSON.parse(storeConfigEl.content));
            } else {
                // TODO: deprecated - the store configuration on the <body> has been deprecated and will be removed
                const { storeView, graphqlEndpoint, graphqlMethod, httpHeaders } = document.body.dataset;
                window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({
                    graphqlEndpoint,
                    storeView,
                    graphqlMethod,
                    headers: httpHeaders ? JSON.parse(httpHeaders) : {}
                });
            }
        } catch (e) {
            console.error(e.message, e);
        } finally {
            document.dispatchEvent(new CustomEvent('aem.cif.clientlib-initialized'));
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export { PriceFormatter, CommerceGraphqlApi };
