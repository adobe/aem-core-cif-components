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

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import cart from '../i18n/en/cart.json';
import checkout from '../i18n/en/checkout.json';
import common from '../i18n/en/common.json';

i18n.use(initReactI18next).init({
    fallbackLng: 'en',
    debug: false,
    lng: 'en',

    interpolation: {
        escapeValue: false,
        format: (value, format, lng) => {
            if (format === 'price') {
                return new Intl.NumberFormat(lng, { style: 'currency', currency: value.currency }).format(value.value);
            }
            return value;
        }
    },

    resources: {
        en: {
            cart,
            checkout,
            common
        }
    }
});

export default i18n;
