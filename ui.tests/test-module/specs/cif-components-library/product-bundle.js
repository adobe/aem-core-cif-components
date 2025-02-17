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
const fs = require('fs');
const path = require('path');

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

        // Create screenshots directory if it doesn't exist
        const screenshotsDir = path.resolve(__dirname, '../../screenshots');
        if (!fs.existsSync(screenshotsDir)) {
            fs.mkdirSync(screensDir, { recursive: true });
        }
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
    });

    it('can customize a bundle product', () => {
        // Go to the product page
        browser.url(product_page);
        browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/bundle_product_page.png'));

        // Check that the customize button is displayed
        const customizeButton = $(`${product_selector} .productFullDetail__customizeBundle button`);
        expect(customizeButton).toBeDisplayed();
        browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/customize_button_displayed.png'));

        customizeButton.click();
        browser.pause(2000);
        browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/customize_button_clicked.png'));

        // Verify that we get 5 "options" fields
        const options = $$(`${product_selector} .productFullDetail__bundleProduct`);
        expect(options.length).toBe(5);
        browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/options_fields.png'));
    });

});