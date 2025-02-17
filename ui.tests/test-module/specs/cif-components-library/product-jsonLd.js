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
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Create screenshots directory if it doesn't exist
        const screenshotsDir = path.resolve(__dirname, '../../screenshots');
        if (!fs.existsSync(screenshotsDir)) {
            fs.mkdirSync(screenshotsDir, { recursive: true });
        }
    });

    it('can enable/disable JSON-LD, save changes, and verify JSON-LD on the product page', async () => {
        // Step 1: Navigate to the Properties page for the Commerce item
        await browser.url(
            `${config.aem.author.base_url}/mnt/overlay/cif/shell/content/configuration/properties.html?item=%2Fconf%2Fcore-components-examples%2Fsettings%2Fcloudconfigs%2Fcommerce`
        );
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step1_properties_page.png'));

        // Step 2: Wait for all tabs to be visible
        const allTabs = await $$('coral-tab');
        await browser.waitUntil(async () => (await allTabs.length) > 0, {
            timeout: 5000,
            timeoutMsg: 'Tabs did not load in time'
        });
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step2_tabs_visible.png'));

        // Step 3: Find the "Features" tab and select it
        let featuresTab;
        for (const tab of allTabs) {
            const label = await tab.$('coral-tab-label');
            const labelText = await label.getText();
            if (labelText === 'Features') {
                featuresTab = tab;
                break;
            }
        }

        // Step 4: Ensure the "Features" tab is selected
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
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step4_features_tab_selected.png'));

        // Step 5: Locate the "Enable JSON" checkbox by its name attribute using WebDriverIO's $() function
        const enableJsonLdCheckbox = await $('coral-checkbox[name="./enableJsonLd"]');
        await enableJsonLdCheckbox.waitForDisplayed({ timeout: 5000 });
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step5_checkbox_visible.png'));

        // Step 6: Check if the checkbox is disabled (aria-disabled="true" or has the class "is-disabled")
        const isDisabled = await enableJsonLdCheckbox.getAttribute('aria-disabled');
        let classList = await enableJsonLdCheckbox.getAttribute('class');
        classList = classList || ''; // Ensure classList is not null

        const hasDisabledClass = classList.includes('is-disabled');

        // Step 7: Check if the checkbox is selected
        const isChecked = await enableJsonLdCheckbox.isSelected();

        // Step 8: If checkbox is checked, skip the action and save & close
        if (!isChecked) {
            // Step 9: If the checkbox is not checked, check if it is locked (disabled)
            if (isDisabled === 'true' || hasDisabledClass) {
                // Use browser.execute to remove the 'aria-disabled' attribute (using JavaScript in the browser context)
                await browser.execute(checkbox => {
                    checkbox.setAttribute('aria-disabled', 'false'); // Remove aria-disabled attribute
                }, enableJsonLdCheckbox);

                // Enable the checkbox input by removing the disabled attribute from the input element
                const inputCheckbox = await enableJsonLdCheckbox.$('input[type="checkbox"]');
                await browser.execute(checkboxInput => {
                    checkboxInput.removeAttribute('disabled'); // Remove the disabled attribute from the checkbox input
                }, inputCheckbox);
            }

            // Step 10: Now, check the checkbox (whether it was previously locked or not)
            const inputCheckbox = await enableJsonLdCheckbox.$('input[type="checkbox"]');

            // Ensure the checkbox is checked, without unchecking it if already checked
            const isInputChecked = await inputCheckbox.isSelected();
            if (!isInputChecked) {
                await inputCheckbox.click(); // Only click if it's not already checked
            }
        }
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step10_checkbox_checked.png'));

        // Step 11: Save the changes
        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 5000 });
        await saveButton.click();
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step11_changes_saved.png'));

        // Step 12: Navigate to the product page
        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step12_product_page.png'));

        // Step 13: Wait for the page to load (use a specific element to ensure the page is ready)
        await browser.waitUntil(
            async () => (await $('#product-page-element')) !== null, // Replace with an actual element that indicates page load
            { timeout: 5000, timeoutMsg: 'Product page did not load in time' }
        );
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step13_page_loaded.png'));

        // Step 14: Get the raw page source
        const pageSource = await browser.getPageSource();

        // Step 15: Verify if JSON-LD script tag is present in the page source
        if (!pageSource.includes('<script type="application/ld+json">')) {
            await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step15_jsonld_missing.png'));
            throw new Error('Test failed: JSON-LD is missing while Enable JSON checkbox is selected.');
        }
        await browser.saveScreenshot(path.resolve(__dirname, '../../screenshots/step15_jsonld_present.png'));
    });
});