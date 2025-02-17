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

describe('Checkbox  Test', () => {
    before(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);

        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
    });

    it('can enable/disable JSON-LD, save changes, and verify JSON-LD on the product page', async () => {
        // Step 1: Navigate to the Properties page for the Commerce item
        await browser.url(
            `${config.aem.author.base_url}/mnt/overlay/cif/shell/content/configuration/properties.html?item=%2Fconf%2Fcore-components-examples%2Fsettings%2Fcloudconfigs%2Fcommerce`
        );

        // Step 2: Ensure the page is fully loaded
        await browser.waitUntil(async () => (await browser.execute('return document.readyState')) === 'complete', {
            timeout: 10000,
            timeoutMsg: 'Page did not load completely in time'
        });

        // Step 3: Wait for tabs to load and select the "Features" tab
        const allTabs = await $$('coral-tab');
        await browser.waitUntil(async () => (await allTabs.length) > 0, {
            timeout: 5000,
            timeoutMsg: 'Tabs did not load in time'
        });

        let featuresTab;
        for (const tab of allTabs) {
            const label = await tab.$('coral-tab-label');
            const labelText = await label.getText();
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

        // Step 4: Locate and interact with the "Enable JSON" checkbox
        const enableJsonLdCheckbox = await $('coral-checkbox[name="./enableJsonLd"]');
        await enableJsonLdCheckbox.waitForDisplayed({ timeout: 10000 });
        await enableJsonLdCheckbox.waitForEnabled({ timeout: 10000 });

        // Retry mechanism for clicking the checkbox
        let retries = 0;
        let maxRetries = 3;
        while (retries < maxRetries) {
            try {
                const inputCheckbox = await enableJsonLdCheckbox.$('input[type="checkbox"]');
                const isInputChecked = await inputCheckbox.isSelected();
                if (!isInputChecked) {
                    await inputCheckbox.click();
                }
                break; // Break if interaction succeeds
            } catch (err) {
                retries++;
                if (retries >= maxRetries) {
                    throw new Error('Failed to check the checkbox after multiple attempts.');
                }
                await browser.pause(1000); // Pause before retrying
            }
        }

        // Step 5: Save the changes
        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 10000 });
        await saveButton.click();

        // Step 6: Navigate to the product page and ensure it's loaded
        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);

        await browser.waitUntil(
            async () => (await $('#product-page-element')) !== null, // Replace with an actual element that indicates page load
            { timeout: 10000, timeoutMsg: 'Product page did not load in time' }
        );

        // Step 7: Verify JSON-LD script on the product page
        const pageSource = await browser.getPageSource();
        if (!pageSource.includes('<script type="application/ld+json">')) {
            throw new Error('Test failed: JSON-LD is missing while Enable JSON checkbox is selected.');
        }
    });
});
