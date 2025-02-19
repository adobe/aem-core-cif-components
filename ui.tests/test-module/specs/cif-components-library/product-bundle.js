/*
 *  Copyright 2021 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

const config = require('../../lib/config');
const commons = require('../../lib/commons');

describe('Product bundle in CIF components library', () => {
    // ✅ Define the product page and selector variables
    const product_page = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/sprite-yoga-companion-kit.html`;
    const product_selector = '.cmp-examples-demo__top .product';

    before(() => {
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Setup GraphQL client
        commons.configureExamplesGraphqlClient(browser);
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
    });

    it('can customize a bundle product', async () => {
        console.log('Navigating to:', product_page);
        await browser.url(product_page);

        // ✅ Ensure page has fully loaded
        await browser.waitUntil(async () => (await browser.execute(() => document.readyState)) === 'complete', {
            timeout: 20000,
            timeoutMsg: 'Page did not fully load'
        });
        console.log('Page fully loaded.');

        // ✅ Print current URL to check if redirected
        console.log('Current URL after navigation:', await browser.getUrl());

        // ✅ Wait for product container to exist before checking visibility
        const productContainer = await $(product_selector);

        if (!(await productContainer.isExisting())) {
            throw new Error('❌ Product container is missing! Check if the product page is correct.');
        }
        console.log('✅ Product container exists.');

        // ✅ Increase timeout for container visibility
        await productContainer.waitForDisplayed({
            timeout: 30000,
            timeoutMsg: 'Product container did not load in time'
        });

        console.log('✅ Product container loaded.');

        // ✅ Now proceed with the test (e.g., clicking the Customize button)
        const customizeButton = await $(`${product_selector} .productFullDetail__customizeBundle button`);
        await customizeButton.waitForDisplayed({ timeout: 20000 });

        console.log('✅ Customize button displayed. Clicking now.');
        await customizeButton.click();

        // ✅ Verify 5 bundle options appear
        await browser.waitUntil(
            async () => (await $$(`${product_selector} .productFullDetail__bundleProduct`)).length === 5,
            { timeout: 10000, timeoutMsg: 'Bundle options did not load properly' }
        );

        console.log('✅ Test passed: 5 bundle options are visible.');
    });
});
