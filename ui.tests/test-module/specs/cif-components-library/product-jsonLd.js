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

const config = require('../../lib/config');

describe('Checkbox Uncheck Test', () => {
    before(() => {
        browser.setWindowSize(1280, 960);

        console.log('Logging into AEM...');
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
        console.log('✅ Login successful.');
    });

    it('can enable/disable JSON-LD, save changes, and verify JSON-LD on the product page', async () => {
        console.log('🔄 Navigating to Properties page...');
        await browser.url(
            `${config.aem.author.base_url}/mnt/overlay/cif/shell/content/configuration/properties.html?item=%2Fconf%2Fcore-components-examples%2Fsettings%2Fcloudconfigs%2Fcommerce`
        );

        // Wait for tabs to load
        console.log('⌛ Waiting for tabs...');
        await browser.waitUntil(async () => (await $$('coral-tab')).length > 0, {
            timeout: 10000,
            timeoutMsg: '❌ Tabs did not load in time'
        });

        // Select the "Features" tab
        console.log('🔍 Finding Features tab...');
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
            throw new Error('❌ Features tab not found! Possible DOM change or incorrect selector.');
        }

        console.log('🔄 Selecting Features tab...');
        if ((await featuresTab.getAttribute('aria-selected')) !== 'true') {
            await featuresTab.click();
        }

        await browser.waitUntil(async () => (await featuresTab.getAttribute('aria-selected')) === 'true', {
            timeout: 5000,
            timeoutMsg: '❌ Features tab was not selected in time'
        });

        // Locate JSON-LD checkbox
        console.log('🔍 Finding JSON-LD checkbox...');
        const enableJsonLdCheckbox = await $('coral-checkbox[name="./enableJsonLd"]');
        await enableJsonLdCheckbox.waitForDisplayed({ timeout: 5000 });

        // Check if the checkbox is disabled
        let isDisabled = await enableJsonLdCheckbox.getProperty('disabled');
        let isChecked = await enableJsonLdCheckbox.isSelected();

        // Enable checkbox if disabled
        if (!isChecked && isDisabled) {
            console.log('⚠️ Checkbox is disabled. Enabling it via JavaScript...');
            await browser.execute(checkbox => checkbox.removeAttribute('disabled'), enableJsonLdCheckbox);

            await browser.waitUntil(async () => !(await enableJsonLdCheckbox.getProperty('disabled')), {
                timeout: 5000,
                timeoutMsg: '❌ Checkbox is still disabled after enabling attempt'
            });
        }

        // Ensure checkbox is interactable
        console.log('✅ Ensuring checkbox is interactable...');
        const inputCheckbox = await enableJsonLdCheckbox.$('input[type="checkbox"]');
        await inputCheckbox.waitForDisplayed({ timeout: 5000 });
        await inputCheckbox.waitForEnabled({ timeout: 5000 });

        // Click checkbox if not checked
        console.log('🔘 Checking JSON-LD checkbox...');
        isChecked = await inputCheckbox.isSelected();
        if (!isChecked) {
            await inputCheckbox.click();

            await browser.waitUntil(async () => await inputCheckbox.isSelected(), {
                timeout: 7000,
                timeoutMsg: '❌ Checkbox was not checked in time'
            });

            console.log('✅ Checkbox is now checked.');
        } else {
            console.log('✅ Checkbox was already checked.');
        }

        // Save changes
        console.log('💾 Saving changes...');
        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 5000 });
        await saveButton.waitForEnabled({ timeout: 5000 });
        await saveButton.click();

        // Allow time for save to process
        await browser.pause(2000);

        // Navigate to product page
        console.log('🔄 Navigating to product page...');
        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);

        // Wait for the URL to change before verifying elements
        await browser.waitUntil(
            async () => (await browser.getUrl()).includes('/library/commerce/product/sample-product.html'),
            { timeout: 10000, timeoutMsg: '❌ Product page URL did not load in time' }
        );

        // Short delay to allow rendering
        await browser.pause(2000);

        console.log('✅ Product page fully loaded.');

        // Verify JSON-LD script presence
        console.log('🔍 Verifying JSON-LD script presence...');
        const pageSource = await browser.getPageSource();
        if (!pageSource.includes('<script type="application/ld+json">')) {
            throw new Error('❌ Test failed: JSON-LD is missing while Enable JSON checkbox is selected.');
        }

        console.log('🎉 Test Passed: JSON-LD is present.');
    });
});
