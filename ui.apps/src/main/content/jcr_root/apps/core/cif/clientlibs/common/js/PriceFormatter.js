/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
'use strict';

class PriceFormatter {
    constructor(locale) {
        this._locale = locale;
        this._formatter = null;
    }

    formatPrice(price) {
        if (!this._formatter) {
            this._formatter = new Intl.NumberFormat(this._locale, {
                style: 'currency',
                currency: price.currency
            });
        }
        return this._formatter.format(price.value);
    }
}

(function() {
    function onDocumentReady() {
        window.CIF = window.CIF || {};
        window.CIF.PriceFormatter = PriceFormatter;
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export default PriceFormatter;
