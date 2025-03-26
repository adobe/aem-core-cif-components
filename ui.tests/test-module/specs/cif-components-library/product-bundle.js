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
        browser.setWindowSize(1280, 960);
    });

    it('can customize a bundle product', () => {
        // Max number of retries
        let retries = 3;
        let pageLoadedSuccessfully = false;

        while (retries > 0 && !pageLoadedSuccessfully) {
            try {
                // Go to the product page
                browser.url(product_page);

                // Scroll to the "Sprite Yoga Companion Kit" title
                const titleElement = $('h1=Sprite Yoga Companion Kit'); // Find the title by its exact text
                titleElement.scrollIntoView(); // Scroll the page to the title

                // Wait for the title to be in view (for debugging purposes)
                browser.pause(1000);

                // Try finding the "Customize" button using the normal selector
                let customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);

                // If the button is not found, try finding it by the button text
                if (!customizeButton.isExisting()) {
                    customizeButton = $('button=Customize'); // Find the button by its text content
                }

                // Ensure the button is displayed and interactable
                if (customizeButton.isDisplayedInViewport()) {
                    // Scroll to the button if it's not in view
                    customizeButton.scrollIntoView();

                    // Wait for the button to be clickable and click it
                    customizeButton.waitForClickable({ timeout: 20000 });
                    customizeButton.click();

                    // Pause to allow any post-click actions to complete
                    browser.pause(2000);

                    // Check for bundle product options after clicking the button
                    const options = $$(`${product_selector} .productFullDetail__bundleProduct`);

                    // Ensure there are exactly 5 options available
                    expect(options.length).toBe(5);

                    // If everything works, mark page as successfully loaded
                    pageLoadedSuccessfully = true;
                } else {
                    throw new Error('Customize button is not visible in the viewport!');
                }
            } catch (error) {
                retries--; // Decrease retry count

                // If retries left, refresh the page and try again
                if (retries > 0) {
                    browser.refresh();
                    browser.pause(3000); // Wait for 3 seconds before retrying
                }
            }
        }

        // Fail the test if the page is not loaded successfully after retries
        if (!pageLoadedSuccessfully) {
            throw new Error('Page could not be loaded successfully after 3 retries.');
        }
    });
});
