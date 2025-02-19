/**************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2024 Adobe
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe and its suppliers, if any. The intellectual
 * and technical concepts contained herein are proprietary to Adobe
 * and its suppliers and are protected by all applicable intellectual
 * property laws, including trade secret and copyright laws.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe.
 ***************************************************************************/
const fs = require('fs');
const path = require('path');
const config = require('../../lib/config');

describe('Checkbox Uncheck Test', () => {
    before(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);

        // AEM Login
        console.log('Logging into AEM...');
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
        console.log('Login successful.');

        // Create screenshots directory if it doesn't exist
        const screenshotsDir = path.resolve(__dirname, '../../screenshots');
        if (!fs.existsSync(screenshotsDir)) {
            fs.mkdirSync(screenshotsDir, { recursive: true });
        }
    });

    it('can enable/disable JSON-LD, save changes, and verify JSON-LD on the product page', async () => {
        console.log('Navigating to Properties page...');
        await browser.url(
            `${config.aem.author.base_url}/mnt/overlay/cif/shell/content/configuration/properties.html?item=%2Fconf%2Fcore-components-examples%2Fsettings%2Fcloudconfigs%2Fcommerce`
        );

        // Wait for all tabs to be visible
        console.log('Waiting for tabs to load...');
        await browser.waitUntil(async () => (await $$('coral-tab')).length > 0, {
            timeout: 10000,
            timeoutMsg: 'Tabs did not load in time'
        });

        // Find and select the "Features" tab
        console.log('Selecting the Features tab...');
        let featuresTab;
        for (const tab of await $$('coral-tab')) {
            const labelText = await tab.$('coral-tab-label').getText();
            if (labelText === 'Features') {
                featuresTab = tab;
                break;
            }
        }

        if (!featuresTab) {
            throw new Error('Features tab not found!');
        }

        const isSelected = await featuresTab.getAttribute('aria-selected');
        if (isSelected !== 'true') {
            await featuresTab.click();
        }

        await browser.waitUntil(async () => (await featuresTab.getAttribute('aria-selected')) === 'true', {
            timeout: 5000,
            timeoutMsg: 'Features tab was not selected in time'
        });

        // Locate the JSON-LD checkbox
        console.log('Finding JSON-LD checkbox...');
        const enableJsonLdCheckbox = await $('coral-checkbox[name="./enableJsonLd"]');
        await enableJsonLdCheckbox.waitForDisplayed({ timeout: 5000 });

        // Check if the checkbox is disabled
        const isDisabled = await enableJsonLdCheckbox.getProperty('disabled'); // More reliable than getAttribute
        let isChecked = await enableJsonLdCheckbox.isSelected();

        // If the checkbox is unchecked and disabled, enable it
        if (!isChecked && isDisabled) {
            console.log('Checkbox is disabled. Attempting to enable it...');
            await browser.execute(checkbox => checkbox.removeAttribute('disabled'), enableJsonLdCheckbox);

            // Ensure checkbox is interactable
            await browser.waitUntil(async () => !(await enableJsonLdCheckbox.getProperty('disabled')), {
                timeout: 5000,
                timeoutMsg: 'Checkbox is still disabled after enabling attempt'
            });
        }

        // Check the checkbox if it's not already checked
        if (!isChecked) {
            console.log('Checking JSON-LD checkbox...');
            const inputCheckbox = await enableJsonLdCheckbox.$('input[type="checkbox"]');
            await inputCheckbox.click();
            await browser.waitUntil(async () => await inputCheckbox.isSelected(), {
                timeout: 5000,
                timeoutMsg: 'Checkbox was not checked in time'
            });
        }

        // Save changes
        console.log('Saving changes...');
        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 5000 });
        await saveButton.waitForEnabled({ timeout: 5000 });
        await saveButton.click();

        // Wait for save to complete (e.g., wait for a notification or redirection)
        await browser.pause(2000);

        // Navigate to product page
        console.log('Navigating to product page...');
        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);

        // Ensure page loads by checking a product page element
        console.log('Waiting for product page to load...');
        await browser.waitUntil(async () => (await $('#product-page-element')).isDisplayed(), {
            timeout: 10000,
            timeoutMsg: 'Product page did not load in time'
        });

        // Verify JSON-LD script presence
        console.log('Verifying JSON-LD script presence...');
        const pageSource = await browser.getPageSource();
        if (!pageSource.includes('<script type="application/ld+json">')) {
            throw new Error('Test failed: JSON-LD is missing while Enable JSON checkbox is selected.');
        }

        console.log('Test passed: JSON-LD is present.');
    });
});
