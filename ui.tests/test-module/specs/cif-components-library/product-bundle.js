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
        // Get current screen size
        const { width, height } = browser.getWindowSize();

        // Reduce the screen size to 25% of the current width and height
        const newWidth = width * 0.25;
        const newHeight = height * 0.25;

        // Set the new window size to 25% of the current dimensions
        browser.setWindowSize(newWidth, newHeight);

        // Take a screenshot after resizing the window
        browser.saveScreenshot('./screenshots/resized_window.png');
    });

    it('can customize a bundle product', () => {
        // Go to the product page
        browser.url(product_page);

        // Take a screenshot before interacting with the page
        browser.saveScreenshot('./screenshots/product_page_before.png');

        // Increase the wait time to ensure elements are fully loaded
        const customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);

        // Wait for the button to be displayed with an increased timeout (20 seconds)
        customizeButton.waitForDisplayed({ timeout: 20000 });

        // Check if the button is displayed and is in the viewport
        expect(customizeButton.isDisplayedInViewport()).toBe(true);

        // Scroll to the button if it's not already in view
        customizeButton.scrollIntoView();

        // Ensure the button is now interactable and click it
        customizeButton.waitForClickable({ timeout: 20000 });
        customizeButton.click();

        // Take a screenshot after clicking the button (e.g., after opening the customization options)
        browser.saveScreenshot('./screenshots/product_page_after_click.png');

        // Pause to allow any post-click actions to complete
        browser.pause(2000);

        // Check for bundle product options after clicking the button
        const options = $$(`${product_selector} .productFullDetail__bundleProduct`);

        // Ensure there are exactly 5 options available
        expect(options.length).toBe(5);

        // Take a screenshot after verifying the options
        browser.saveScreenshot('./screenshots/product_options_after_verification.png');
    });
});
