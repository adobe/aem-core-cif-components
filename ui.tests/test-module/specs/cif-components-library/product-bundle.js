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

    // Debug current URL after navigation
    await browser.pause(2000); // Small delay to allow URL to update
    console.log('Current URL after navigation:', await browser.getUrl());

    // Increase timeout for page load
    await browser.waitUntil(async () => (await browser.getUrl()) === product_page, {
        timeout: 20000,
        timeoutMsg: 'Product page did not load in time'
    });

    console.log('Product page loaded successfully.');

    // Wait for the customize button
    const customizeButton = await $(`${product_selector} .productFullDetail__customizeBundle button`);
    await customizeButton.waitForDisplayed({ timeout: 10000 });

    // Click the button
    expect(customizeButton).toBeDisplayed();
    await customizeButton.click();

    // Verify bundle options appear
    const options = await $$(`${product_selector} .productFullDetail__bundleProduct`);
    await browser.waitUntil(() => options.length === 5, {
        timeout: 10000,
        timeoutMsg: 'Options did not load properly'
    });

    expect(options.length).toBe(5);
});
