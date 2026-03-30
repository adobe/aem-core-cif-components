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

describe('Product Carousel component in CIF components library', () => {
    const productcarousel_page = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/productcarousel.html`;
    const productcarousel_selector = '.cmp-examples-demo__top .productcarousel';

    before(() => {
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Setup GraphQL client
        commons.configureExamplesGraphqlClient(browser);
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
    });

    it('can click navigation arrows in carousel', () => {
        browser.url(productcarousel_page);

        $(`${productcarousel_selector}`).waitForExist({ timeout: 90000 });

        browser.waitUntil(
            () => $$(`${productcarousel_selector} .product__card`).length > 0,
            {
                timeout: 90000,
                interval: 250,
                timeoutMsg: 'Carousel product cards did not load (GraphQL)'
            }
        );

        const rightButton = $(`${productcarousel_selector} .productcarousel__btn--next`);
        rightButton.waitForDisplayed({ timeout: 90000 });

        let product = $(`${productcarousel_selector} .product__card[data-product-sku="MH01-XS-Orange"]`);
        product.waitForExist({ timeout: 30000 });

        expect(product.getLocation('x') < rightButton.getLocation('x')).toBe(false);

        // Click right/next arrow
        rightButton.click();

        browser.waitUntil(() => product.getLocation('x') < rightButton.getLocation('x'), {
            timeout: 15000,
            interval: 100,
            timeoutMsg: 'Carousel did not scroll after clicking next'
        });

        expect(product.getLocation('x') < rightButton.getLocation('x')).toBe(true);
    });

    it('exposes the SKU of the products', () => {
        browser.url(productcarousel_page);

        $(`${productcarousel_selector}`).waitForExist({ timeout: 90000 });

        browser.waitUntil(
            () => $$(`${productcarousel_selector} .product__card`).length > 0,
            { timeout: 90000, interval: 250, timeoutMsg: 'No product cards rendered in carousel' }
        );

        $$(`${productcarousel_selector} .product__card`).forEach((card) => {
            expect(card).toHaveAttribute('data-product-sku');
        });
    });
});
