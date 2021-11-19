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

let storeRootUrl = null;

export const createProductPageUrl = sku => {
    const url = new URL(window.location);

    // Provided by StoreConfigExporter
    if (storeRootUrl) {
        return storeRootUrl;
    }

    let storeConfigEl = document.querySelector('meta[name="store-config"]');
    let pathname;

    if (storeConfigEl) {
        pathname = JSON.parse(storeConfigEl.content).storeRootUrl;
    } else {
        // TODO: deprecated - the store configuration on the <body> has been deprecated and will be removed
        pathname = document.body.dataset.storeRootUrl;
    }

    if (!pathname) {
        return null;
    }

    const extension = '.html';
    const path = pathname.substr(0, pathname.lastIndexOf('.'));

    url.pathname = `${path}.cifproductredirect${extension}/${sku}`;

    return (storeRootUrl = url.toString());
};
