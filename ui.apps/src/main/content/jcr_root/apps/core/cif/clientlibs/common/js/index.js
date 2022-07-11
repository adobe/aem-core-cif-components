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
import PriceToPriceModelConverter from './PriceToPriceModelConverter';

(function() {
    function onDocumentReady() {
        try {
            window.CIF = window.CIF || {};

            // define PriceFormatter
            window.CIF.PriceFormatter = PriceFormatter;

            // define PriceToPriceRangeConverter
            window.CIF.PriceToPriceModelConverter = PriceToPriceModelConverter;

            // initialize CommerceGraphqlApi
            const storeConfigEl = document.querySelector('meta[name="store-config"]');

            if (storeConfigEl) {
                const config = JSON.parse(storeConfigEl.content);
                window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi(config);
                window.CIF.locale = config.locale;
                window.CIF.enableClientSidePriceLoading = config.enableClientSidePriceLoading || false;
            } else {
                // TODO: deprecated - the store configuration on the <body> has been deprecated and will be removed
                const { storeView, graphqlEndpoint, graphqlMethod, httpHeaders } = document.body.dataset;
                window.CIF.CommerceGraphqlApi = new CommerceGraphqlApi({
                    graphqlEndpoint,
                    storeView,
                    graphqlMethod,
                    headers: httpHeaders ? JSON.parse(httpHeaders) : {}
                });
                window.CIF.enableClientSidePriceLoading = false;
            }

            // suspend price fetching until all handlers of aem.cif.clientlib-initialized were called
            window.CIF.CommerceGraphqlApi._suspendGetProductPrices();
        } catch (e) {
            console.error(e.message, e);
        } finally {
            // Dispatch the aem.cif.clientlib-initialized event to signal to any async loaded components that the CIF frontend APIs are
            // available. Components are save to assume that this event is only fired AFTER the DOMCOntentLoaded event it listening for
            // both is not necessary. This allows us to use plain, synchronous event listeners instead of Promise.all().
            document.dispatchEvent(new CustomEvent('aem.cif.clientlib-initialized'));

            if (window.CIF.CommerceGraphqlApi) {
                // resume price fetching
                window.CIF.CommerceGraphqlApi._resumeGetProductPrices();
            }
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export { PriceFormatter, CommerceGraphqlApi };
