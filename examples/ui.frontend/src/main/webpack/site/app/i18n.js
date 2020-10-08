/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import Backend from 'i18next-xhr-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n.use(Backend)
    .use(LanguageDetector)
    .use(initReactI18next)

    .init({
        fallbackLng: 'en-US',
        debug: true,

        load: 'currentOnly',
        defaultNS: 'common',
        ns: [],

        interpolation: {
            escapeValue: false,
            format: (value, format, lng) => {
                if (format === 'price') {
                    return new Intl.NumberFormat(lng, {
                        style: 'currency',
                        currency: value.currency
                    }).format(value.value);
                }
                return value;
            }
        },

        detection: {
            order: ['htmlTag', 'path', 'subdomain'],

            lookupFromPathIndex: 1,
            lookupFromSubdomainIndex: 0
        },

        backend: {
            loadPath: '/etc.clientlibs/venia/clientlibs/clientlib-site/resources/i18n/{{lng}}/{{ns}}.json',
            allowMultiLoading: false,
            withCredentials: true
        }
    });

export default i18n;
