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
'use strict';

const processProductStorefrontData = (mse) => {
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

const processSearchInputStorefrontData = (mse) => {
    const searchInputCtxElement = document.querySelector('[data-cif-search-input-context]');
    if (searchInputCtxElement) {
        try {
            const searchInputCtx = JSON.parse(searchInputCtxElement.dataset.cifSearchInputContext);
            mse.context.setSearchInput({ units: [searchInputCtx] });
        } catch (e) {
            console.error(e);
        }
    }
};

const processSearchResultsStorefrontData = (mse) => {
    const searchResultsCtxElement = document.querySelector('[data-cif-search-results-context]');
    if (searchResultsCtxElement) {
        try {
            const searchResultsCtx = JSON.parse(searchResultsCtxElement.dataset.cifSearchResultsContext);
            mse.context.setSearchResults({ units: [searchResultsCtx] });
        } catch (e) {
            console.error(e);
        }
    }
};

const processCategoryStorefrontData = (mse) => {
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

const setStorefrontContexts = (mse) => {
    processProductStorefrontData(mse);
    processSearchInputStorefrontData(mse);
    processSearchResultsStorefrontData(mse);
    processCategoryStorefrontData(mse);
};

document.addEventListener('mse-loaded', function() {
    console.log('mse-loaded');
    const mse = window.magentoStorefrontEvents;

    setStorefrontContexts(mse);
})

