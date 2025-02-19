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
    const product_page = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/sprite-yoga-companion-kit.html`;
    const product_selector = '.cmp-examples-demo__top .product';

    before(() => {
        console.log('Logging into AEM...');
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
        console.log('Login successful.');

        // Setup GraphQL client
        commons.configureExamplesGraphqlClient(browser);
    });

    beforeEach(() => {
        browser.setWindowSize(1280, 960);
    });

    it('can customize a bundle product', () => {
        console.log('Navigating to product page...');
        browser.url(product_page);

        // Ensure the customize button is visible before interacting
        console.log('Waiting for the Customize button...');
        const customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);
        customizeButton.waitForDisplayed({ timeout: 10000, timeoutMsg: 'Customize button did not appear in time' });

        // Ensure button is in view and clickable
        customizeButton.scrollIntoView();
        customizeButton.waitForClickable({ timeout: 5000, timeoutMsg: 'Customize button is not clickable' });

        console.log('Clicking Customize button...');
        customizeButton.click();

        // Wait for 5 product options to load
        console.log('Waiting for product options to load...');
        browser.waitUntil(() => $$(`${product_selector} .productFullDetail__bundleProduct`).length === 5, {
            timeout: 10000,
            timeoutMsg: 'Not all 5 options loaded in time'
        });

        const options = $$(`${product_selector} .productFullDetail__bundleProduct`);
        expect(options.length).toBe(5);
        console.log('Bundle customization options loaded successfully.');
    });
});
