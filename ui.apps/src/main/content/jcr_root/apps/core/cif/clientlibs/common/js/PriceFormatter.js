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

import DOMPurify from 'dompurify';

class PriceFormatter {
    constructor(locale) {
        this._locale = locale || window.CIF.locale || document.documentElement.lang || navigator.language;
        this._formatter = null;
        this._i18n = null;
    }

    formatPriceAsHtml(price, { showDiscountPercentage = false, showStartingAt = false } = {}) {
        let innerHTML = '';

        showStartingAt = showStartingAt && price.productType === 'GroupedProduct';

        if (!price.range) {
            if (price.discounted) {
                innerHTML += `<span class="regularPrice">${this.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })}</span>
                    <span class="discountedPrice">${this.formatPrice({
                        value: price.finalPrice,
                        currency: price.currency
                    })}</span>`;

                if (showDiscountPercentage) {
                    let youSave = this.get('You save');
                    innerHTML += ` <span class="you-save">${youSave} ${this.formatPrice({
                        value: price.discountAmount,
                        currency: price.currency
                    })} (${price.discountPercent}%)</span>`;
                }
            } else {
                let prefix = showStartingAt ? this.get('Starting at') + ' ' : '';
                innerHTML += `<span>${prefix}${this.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })}</span>`;
            }
        } else {
            let from = this.get('From');
            let to = this.get('To');
            if (price.discounted) {
                innerHTML += `<span class="regularPrice">${from} ${this.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })} ${to} ${this.formatPrice({
                    value: price.regularPriceMax,
                    currency: price.currency
                })}</span>
                    <span class="discountedPrice">${from} ${this.formatPrice({
                    value: price.finalPrice,
                    currency: price.currency
                })} ${to} ${this.formatPrice({
                    value: price.finalPriceMax,
                    currency: price.currency
                })}</span>`;

                if (showDiscountPercentage) {
                    let youSave = this.get('You save');
                    innerHTML += `<span class="you-save">${youSave} ${this.formatPrice({
                        value: price.discountAmount,
                        currency: price.currency
                    })} (${price.discountPercent}%)</span>`;
                }
            } else {
                innerHTML += `<span>${from} ${this.formatPrice({
                    value: price.regularPrice,
                    currency: price.currency
                })} ${to} ${this.formatPrice({
                    value: price.regularPriceMax,
                    currency: price.currency
                })}</span>`;
            }
        }
        return DOMPurify.sanitize(innerHTML);
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
