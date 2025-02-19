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

    it('can customize a bundle product', () => {
        // Go to the product page
        console.log('Navigating to:', product_page);
        browser.url(product_page);

        // Ensure the product container is visible before proceeding
        const productContainer = $(product_selector);
        productContainer.waitForDisplayed({
            timeout: 10000,
            timeoutMsg: 'Product page did not load in time'
        });

        // Wait for the customize button
        const customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);
        customizeButton.waitForDisplayed({
            timeout: 10000,
            timeoutMsg: 'Customize button did not appear'
        });

        console.log('Customize button found. Clicking...');
        expect(customizeButton).toBeDisplayed();
        customizeButton.click();

        // Wait until the bundle options appear
        browser.waitUntil(
            () => {
                const options = $$(`${product_selector} .productFullDetail__bundleProduct`);
                return options.length === 5;
            },
            {
                timeout: 10000,
                timeoutMsg: 'Options did not load properly'
            }
        );

        // Get the options after waiting
        const options = $$(`${product_selector} .productFullDetail__bundleProduct`);

        // Ensure 5 options are present and visible
        expect(options.length).toBe(5);
        options.forEach(option => {
            expect(option.isDisplayed()).toBe(true);
        });

        console.log('Test passed: 5 bundle options are visible');
    });
});
