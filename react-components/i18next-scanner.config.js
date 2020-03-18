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

module.exports = {
    input: [
        'src/**/*.js',
        '!src/**/*.test.js',
        '!./i18n/**',
        '!**/node_modules/**',
    ],
    output: './',
    options: {
        debug: true,
        func: {
            list: ['i18next.t', 'i18n.t', 't'],
            extensions: ['.js', '.jsx']
        },
        trans: {
            component: 'Trans',
            i18nKey: 'i18nKey',
            defaultsKey: 'defaults',
            extensions: ['.js', '.jsx'],
            fallbackKey: false,
            acorn: {
                ecmaVersion: 10,
                sourceType: 'module'
            }
        },
        lngs: ['en'],
        ns: ['cart', 'checkout', 'account', 'common'],
        defaultLng: 'en',
        defaultNs: 'common',
        defaultValue: '__STRING_NOT_TRANSLATED__',
        resource: {
            loadPath: 'i18n/{{lng}}/{{ns}}.json',
            savePath: 'i18n/{{lng}}/{{ns}}.json',
            jsonIndent: 2,
            lineEnding: '\n'
        },
        nsSeparator: ':',
        keySeparator: '.',
        pluralSeparator: '_',
        contextSeparator: '_',
        interpolation: {
            prefix: '{{',
            suffix: '}}'
        }
    }
};