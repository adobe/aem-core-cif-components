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

it('can customize a bundle product', async () => {
    console.log('Navigating to:', product_page);
    await browser.url(product_page);

    // Wait for product container to load before checking button
    const productContainer = await $(product_selector);
    await productContainer.waitForDisplayed({ timeout: 15000, timeoutMsg: 'Product container did not load in time' });

    console.log('Product container loaded.');

    // Locate the customize button
    const customizeButton = await $(`${product_selector} .productFullDetail__customizeBundle button`);

    // Check if the button exists before waiting for display
    if (!(await customizeButton.isExisting())) {
        throw new Error('Customize button is not found in the DOM! Check if the product has customization enabled.');
    }
    console.log('Customize button exists in the DOM.');

    // Wait for the button to become visible
    await browser.waitUntil(async () => await customizeButton.isDisplayed(), {
        timeout: 20000,
        timeoutMsg: 'Customize button did not appear in time'
    });

    console.log('Customize button displayed. Clicking now.');
    await customizeButton.click();

    // Wait until the bundle options appear
    await browser.waitUntil(
        async () => (await $$(`${product_selector} .productFullDetail__bundleProduct`)).length === 5,
        { timeout: 10000, timeoutMsg: 'Bundle options did not load properly' }
    );

    console.log('✅ Test passed: 5 bundle options are visible.');
});
