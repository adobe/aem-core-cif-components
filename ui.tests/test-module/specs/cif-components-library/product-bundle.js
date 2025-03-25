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

const fs = require('fs');
const path = require('path');
const config = require('../../lib/config');
const commons = require('../../lib/commons');

// Define the directory path for screenshots
const screenshotsDir = path.join(__dirname, 'screenshots');

// Check if the screenshots directory exists, if not, create it
if (!fs.existsSync(screenshotsDir)) {
    fs.mkdirSync(screenshotsDir, { recursive: true });
}

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
        // No screen resizing logic anymore, only focusing on scrolling and interaction
    });

    it('can customize a bundle product', () => {
        // Go to the product page
        browser.url(product_page);

        // Wait for the page to load and stabilize
        browser.waitUntil(
            () => browser.getTitle() === 'Sprite Yoga Companion Kit', // Ensure the correct page title
            {
                timeout: 20000,
                timeoutMsg: 'Page did not load in time'
            }
        );

        // Take a screenshot before interacting with the page
        browser.saveScreenshot('./screenshots/product_page_before.png');

        // Scroll to the "Sprite Yoga Companion Kit" title
        const titleElement = $('h1=Sprite Yoga Companion Kit'); // Find the title by its exact text
        titleElement.scrollIntoView(); // Scroll the page to the title

        // Wait for the title to be in view (for debugging purposes)
        browser.pause(1000);

        // Take a screenshot after scrolling to the title
        browser.saveScreenshot('./screenshots/title_scrolled_into_view.png');

        // Try finding the "Customize" button using the normal selector
        let customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);

        // If the button is not found, try finding it by the button text
        if (!customizeButton.isExisting()) {
            customizeButton = $(`button=Customize`); // Find the button by its text content
        }

        // Wait for the button to be displayed and ensure it's interactable
        customizeButton.waitForDisplayed({ timeout: 20000 });

        // Take a screenshot after finding the button
        browser.saveScreenshot('./screenshots/button_found.png');

        expect(customizeButton.isDisplayedInViewport()).toBe(true);

        // Scroll to the button if it's not in view
        customizeButton.scrollIntoView();

        // Take a screenshot after scrolling to the button
        browser.saveScreenshot('./screenshots/button_scrolled_into_view.png');

        // Ensure the button is clickable and click it
        customizeButton.waitForClickable({ timeout: 20000 });
        customizeButton.click();

        // Take a screenshot after clicking the button
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
