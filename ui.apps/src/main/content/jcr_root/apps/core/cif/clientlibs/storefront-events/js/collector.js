/*******************************************************************************
 *
 *     Copyright 2021 Adobe. All rights reserved.
 *     This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License. You may obtain a copy
 *     of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under
 *     the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *     OF ANY KIND, either express or implied. See the License for the specific language
 *     governing permissions and limitations under the License.
 *
 ******************************************************************************/
'use strict';

import mse from '@adobe/magento-storefront-events-sdk';

// Expose Magento Storefront Events SDK on the global window object
window.magentoStorefrontEvents = mse;

const processProductStorefrontData = () => {
    const productCtxElement = document.querySelector('[data-cif-product-context]');
    if (productCtxElement) {
        try {
            const productCtx = JSON.parse(productCtxElement.dataset.cifProductContext);
            mse.context.setProduct(productCtx);
            mse.publish.productPageView();
        } catch (e) {
            console.error(e);
        }
    }
};

const processCategoryStorefrontData = () => {
    const categoryCtxElement = document.querySelector('[data-cif-category-context]');
    if (categoryCtxElement) {
        try {
            const categoryCtx = JSON.parse(categoryCtxElement.dataset.cifCategoryContext);
            mse.context.setCategory(categoryCtx);
        } catch (e) {
            console.error(e);
        }
    }
};

processProductStorefrontData();
processCategoryStorefrontData();
const onDocumentReady = () => {
    processProductStorefrontData();
};

if (document.readyState !== 'loading') {
    onDocumentReady();
} else {
    document.addEventListener('DOMContentLoaded', onDocumentReady);
}
