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
    // ✅ FIXED: Use backticks (`) for template literals
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

        // ✅ FIXED: Wait for product page to load
        await browser.waitUntil(async () => (await browser.getUrl()) === product_page, {
            timeout: 20000,
            timeoutMsg: 'Product page did not load in time'
        });

        console.log('Product page loaded successfully.');

        // ✅ FIXED: Use backticks (`) for selector interpolation
        const customizeButton = await $(`${product_selector} .productFullDetail__customizeBundle button`);
        await customizeButton.waitForDisplayed({ timeout: 10000 });

        console.log('Customize button found. Clicking...');
        await customizeButton.click();

        // ✅ FIXED: Replaced pause with waitUntil
        await browser.waitUntil(
            async () => (await $$(`${product_selector} .productFullDetail__bundleProduct`)).length === 5,
            { timeout: 10000, timeoutMsg: 'Bundle options did not load properly' }
        );

        // Verify that exactly 5 options are present
        const options = await $$(`${product_selector} .productFullDetail__bundleProduct`);
        expect(options.length).toBe(5);
        console.log('✅ Test passed: 5 bundle options are visible.');
    });
});
