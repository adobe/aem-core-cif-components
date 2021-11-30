/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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

export const createProductPageUrl = sku => {
    const url = new URL(window.location);

    // Provided by StoreConfigExporter
    let storeConfigEl = document.querySelector('meta[name="store-config"]');
    let storeRootUrl;

    if (storeConfigEl) {
        storeRootUrl = JSON.parse(storeConfigEl.content).storeRootUrl;
    } else {
        // TODO: deprecated - the store configuration on the <body> has been deprecated and will be removed
        storeRootUrl = document.body.dataset.storeRootUrl;
    }

    if (!storeRootUrl) {
        return null;
    }

    const path = storeRootUrl.substr(0, storeRootUrl.lastIndexOf('.'));
    url.pathname = `${path}.cifproductredirect.html/${sku}`;

    return url.toString();
};
