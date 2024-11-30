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

        // Step 2: Wait for all tabs to be visible
        const allTabs = await $$('coral-tab');
        await browser.waitUntil(async () => (await allTabs.length) > 0, {
            timeout: 5000,
            timeoutMsg: 'Tabs did not load in time'
        });

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

        // Step 5: Locate the "Enable JSON" checkbox by its name attribute using WebDriverIO's $() function
        const enableJsonCheckbox = await $('coral-checkbox[name="./enableJson"]');
        await enableJsonCheckbox.waitForDisplayed({ timeout: 5000 });

        // Step 6: Check if the checkbox is disabled (aria-disabled="true" or has the class "is-disabled")
        const isDisabled = await enableJsonCheckbox.getAttribute('aria-disabled');
        let classList = await enableJsonCheckbox.getAttribute('class');
        classList = classList || ''; // Ensure classList is not null

        const hasDisabledClass = classList.includes('is-disabled');

        // Step 7: Check if the checkbox is selected
        const isChecked = await enableJsonCheckbox.isSelected();

        // Step 8: If checkbox is checked, skip the action and save & close
        if (!isChecked) {
            // Step 9: If the checkbox is not checked, check if it is locked (disabled)
            if (isDisabled === 'true' || hasDisabledClass) {
                // Use browser.execute to remove the 'aria-disabled' attribute (using JavaScript in the browser context)
                await browser.execute(checkbox => {
                    checkbox.setAttribute('aria-disabled', 'false'); // Remove aria-disabled attribute
                }, enableJsonCheckbox);

                // Enable the checkbox input by removing the disabled attribute from the input element
                const inputCheckbox = await enableJsonCheckbox.$('input[type="checkbox"]');
                await browser.execute(checkboxInput => {
                    checkboxInput.removeAttribute('disabled'); // Remove the disabled attribute from the checkbox input
                }, inputCheckbox);
            }

            // Step 10: Now, check the checkbox (whether it was previously locked or not)
            const inputCheckbox = await enableJsonCheckbox.$('input[type="checkbox"]');

            // Ensure the checkbox is checked, without unchecking it if already checked
            const isInputChecked = await inputCheckbox.isSelected();
            if (!isInputChecked) {
                await inputCheckbox.click(); // Only click if it's not already checked
            }
        }

        // Step 11: Save the changes
        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 5000 });
        await saveButton.click();

        // Step 12: Navigate to the product page
        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);

        // Step 13: Wait for the page to load (use a specific element to ensure the page is ready)
        await browser.waitUntil(
            async () => (await $('#product-page-element')) !== null, // Replace with an actual element that indicates page load
            { timeout: 5000, timeoutMsg: 'Product page did not load in time' }
        );

        // Step 14: Get the raw page source
        const pageSource = await browser.getPageSource();

        // Step 15: Verify if JSON-LD script tag is present in the page source
        if (!pageSource.includes('<script type="application/ld+json">')) {
            throw new Error('Test failed: JSON-LD is missing while Enable JSON checkbox is selected.');
        }
    });

    it('can uncheck the checkbox if already checked, save changes, and verify JSON-LD on the product page', async () => {
        // Step 1: Navigate to the Properties page for the Commerce item
        await browser.url(
            `${config.aem.author.base_url}/mnt/overlay/cif/shell/content/configuration/properties.html?item=%2Fconf%2Fcore-components-examples%2Fsettings%2Fcloudconfigs%2Fcommerce`
        );

        // Step 2: Wait for all tabs to be visible
        const allTabs = await $$('coral-tab');
        await browser.waitUntil(async () => (await allTabs.length) > 0, {
            timeout: 5000,
            timeoutMsg: 'Tabs did not load in time'
        });

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

        // Step 5: Locate the "Enable JSON" checkbox by its name attribute using WebDriverIO's $() function
        const enableJsonCheckbox = await $('coral-checkbox[name="./enableJson"]');
        await enableJsonCheckbox.waitForDisplayed({ timeout: 5000 });

        // Step 6: Check the actual checked state of the checkbox
        const inputCheckbox = await enableJsonCheckbox.$('input[type="checkbox"]');
        const isCheckedBefore = await inputCheckbox.isSelected();

        // **Case 1:** If the checkbox is selected (checked), uncheck it
        if (isCheckedBefore) {
            // Try clicking the input checkbox element directly
            await inputCheckbox.click(); // Attempt to uncheck by clicking

            // Wait for the checkbox to become unchecked
            await browser.waitUntil(async () => !(await inputCheckbox.isSelected()), {
                timeout: 5000,
                timeoutMsg: 'Checkbox could not be unchecked in time'
            });

            // **Fallback option:** If clicking does not work, forcefully update the `checked` attribute
            if (await inputCheckbox.isSelected()) {
                await inputCheckbox.setAttribute('checked', 'false'); // Directly change the checked attribute
            }
        }

        // Step 7: Save & close
        const saveButton = await $('#shell-propertiespage-doneactivator');
        await saveButton.waitForDisplayed({ timeout: 5000 });
        await saveButton.click();

        // Step 8: Navigate to the product page
        const productPageUrl = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html?wcmmode=disabled`;
        await browser.url(productPageUrl);

        // Step 9: Wait for the page to load (use a specific element to ensure the page is ready)
        await browser.waitUntil(
            async () => (await $('#product-page-element')) !== null, // Replace with an actual element that indicates page load
            { timeout: 5000, timeoutMsg: 'Product page did not load in time' }
        );

        // Step 10: Get the raw page source
        const pageSource = await browser.getPageSource();

        // Step 11: Verify if JSON-LD script tag is present in the page source
        if (pageSource.includes('<script type="application/ld+json">')) {
            throw new Error('Test failed: JSON-LD is present in the page source while checkbox is unchecked.');
        }
    });
});
