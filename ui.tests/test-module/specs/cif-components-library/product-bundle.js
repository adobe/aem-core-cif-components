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
        browser.url(product_page);

        // Increase the wait time to 20 seconds
        const customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);

        // Wait for the button to be displayed, increasing the timeout
        customizeButton.waitForDisplayed({ timeout: 20000 });

        // Ensure the button is in the viewport
        expect(customizeButton.isDisplayedInViewport()).toBe(true);

        // Scroll to the button if needed
        customizeButton.scrollIntoView();

        // Click the button to proceed
        customizeButton.click();

        browser.pause(2000);

        // Find all the bundle product options
        const options = $$(`${product_selector} .productFullDetail__bundleProduct`);

        // Ensure there are 5 options
        expect(options.length).toBe(5);
    });
});
