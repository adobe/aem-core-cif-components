/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
'use strict';

class PriceFormatter {
    constructor(locale) {
        this._locale = locale;
        this._formatter = null;
        this._i18n = null;
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

    // i18n support for price-related strings
    get(key) {
        if (!this._i18n) {
            if (window.Granite && window.Granite.I18n) {
                window.Granite.I18n.setLocale(this._locale);
                this._i18n = window.Granite.I18n;
            } else {
                this._i18n = { get: key => key }; // If we don't have a dictionary, we simply return the key
            }
        }
        return this._i18n.get(key);
    }
}

export default PriceFormatter;
