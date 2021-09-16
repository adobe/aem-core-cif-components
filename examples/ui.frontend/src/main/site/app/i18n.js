/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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

 const supportedLanguages = ['en'];

// detect locale
let language;
// 1. <html> lang attribute
if (document && document.documentElement && typeof document.documentElement.getAttribute === 'function') {
    language = document.documentElement.getAttribute('lang');
}
// 2. path (index = 1)
if (!language && window) {
    const matches = window.location.pathname.match(/\/([a-zA-Z-]*)/g);
    if (matches instanceof Array && typeof matches[1] === 'string') {
        language = matches[1].replace('/', '');
    }
}
// 3. subdomain (index = 0)
if (!language && window) {
    const matches = window.location.href.match(/(?:http[s]*\:\/\/)*(.*?)\.(?=[^\/]*\..{2,5})/gi);
    if (matches instanceof Array && typeof matches[0] === 'string') {
        language = matches[0].replace('http://', '').replace('https://', '').replace('.', '');
    }
}
// check compatibility
if (language) {
    while (supportedLanguages.indexOf(language) < 0 && language.indexOf('-') > 0) {
        language = language.substr(0, language.lastIndexOf('-'));
    }
}
// fallback
if (supportedLanguages.indexOf(language) < 0) {
    language = 'en';
}

export default async function () {
    return import( /* webpackChunkName: "i18n/[request]" */ `../../i18n/${language}`);
}
