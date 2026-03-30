/*
 *  Copyright 2024 Adobe Systems Incorporated
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

describe('Enable JSON-LD and Verify on Product Page', () => {
    before(() => {
        browser.setWindowSize(1280, 960);

        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
    });

    it('should enable JSON-LD in AEM settings, save changes, and verify its presence on the product page', async () => {
        await browser.url(
            `${config.aem.author.base_url}/mnt/overlay/cif/shell/content/configuration/properties.html?item=%2Fconf%2Fcore-components-examples%2Fsettings%2Fcloudconfigs%2Fcommerce`
        );

        // Wait for tabs to load

        await browser.waitUntil(async () => (await $$('coral-tab')).length > 0, {
            timeout: 10000,
            timeoutMsg: 'Tabs did not load in time'
        });

        // Select the "Features" tab

        let featuresTab;
        for (const tab of await $$('coral-tab')) {
            const labelElement = await tab.$('coral-tab-label');
            if (labelElement && (await labelElement.isExisting())) {
                const labelText = await labelElement.getText();
                if (labelText === 'Features') {
                    featuresTab = tab;
                    break;
                }
            }
        }

        if (!featuresTab) {
            throw new Error('Features tab not found! Possible DOM change or incorrect selector.');
        }

        if ((await featuresTab.getAttribute('aria-selected')) !== 'true') {
            await featuresTab.click();
        }

        await browser.waitUntil(async () => (await featuresTab.getAttribute('aria-selected')) === 'true', {
            timeout: 5000,
            timeoutMsg: ' Features tab was not selected in time'
        });

        // Locate JSON-LD checkbox

        const enableJsonLdCheckbox = await $('coral-checkbox[name="./enableJsonLd"]');
        await enableJsonLdCheckbox.waitForDisplayed({ timeout: 5000 });

        // Check if the checkbox is disabled
        let isDisabled = await enableJsonLdCheckbox.getProperty('disabled');
        let isChecked = await enableJsonLdCheckbox.isSelected();

        // Enable checkbox if disabled
        if (!isChecked && isDisabled) {
            await browser.execute(checkbox => checkbox.removeAttribute('disabled'), enableJsonLdCheckbox);

            await browser.waitUntil(async () => !(await enableJsonLdCheckbox.getProperty('disabled')), {
                timeout: 5000,
                timeoutMsg: 'Checkbox is still disabled after enabling attempt'
            });
        }

        // Ensure checkbox is interactable

        const inputCheckbox = await enableJsonLdCheckbox.$('input[type="checkbox"]');
        await inputCheckbox.waitForDisplayed({ timeout: 5000 });
        await inputCheckbox.waitForEnabled({ timeout: 5000 });

        // Click checkbox if not checked

        isChecked = await inputCheckbox.isSelected();
        if (!isChecked) {
            await inputCheckbox.click();

            await browser.waitUntil(async () => await inputCheckbox.isSelected(), {
                timeout: 7000,
                timeoutMsg: 'Checkbox was not checked in time'
            });
        }

        // Save changes

        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 5000 });
        await saveButton.waitForEnabled({ timeout: 5000 });
        await saveButton.click();

        // Allow time for save to process
        await browser.pause(2000);

        // Navigate to product page

        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);

        // Wait for the URL to change before verifying elements
        await browser.waitUntil(
            async () => (await browser.getUrl()).includes('/library/commerce/product/sample-product.html'),
            { timeout: 10000, timeoutMsg: 'Product page URL did not load in time' }
        );

        // Short delay to allow rendering
        await browser.pause(2000);

        // Verify JSON-LD script presence

        const pageSource = await browser.getPageSource();
        if (!pageSource.includes('<script type="application/ld+json">')) {
            throw new Error('Test failed: JSON-LD is missing while Enable JSON checkbox is selected.');
        }
    });
});
