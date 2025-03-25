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
        browser.setWindowSize(1280, 960);

        // Take a screenshot after resizing the window
        browser.saveScreenshot(path.join(screenshotsDir, 'resized_window.png'));
    });

    it('can customize a bundle product', () => {
        // Go to the product page
        browser.url(product_page);

        // Take a screenshot before interacting with the page
        browser.saveScreenshot(path.join(screenshotsDir, 'product_page_before.png'));

        const title = $('=Sprite Yoga Companion Kit'); // This selects the element with exact text

        // Wait for the title to be visible (increase timeout if necessary)
        title.waitForDisplayed({ timeout: 20000 });

        // Scroll to the title to bring it into view
        title.scrollIntoView();

        // Take a screenshot after scrolling to the title
        browser.saveScreenshot('./screenshots/title_scrolled_into_view.png');

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
        browser.saveScreenshot(path.join(screenshotsDir, 'product_page_after_click.png'));

        // Pause to allow any post-click actions to complete
        browser.pause(2000);

        // Check for bundle product options after clicking the button
        const options = $$(`${product_selector} .productFullDetail__bundleProduct`);

        // Ensure there are exactly 5 options available
        expect(options.length).toBe(5);

        // Take a screenshot after verifying the options
        browser.saveScreenshot(path.join(screenshotsDir, 'product_options_after_verification.png'));
    });
});
