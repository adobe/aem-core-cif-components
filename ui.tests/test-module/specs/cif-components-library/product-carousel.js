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
        // Go to the product carousel page
        browser.url(productcarousel_page);

        // Check that the right/next arrow button is displayed
        const rightButton = $(`${productcarousel_selector} .productcarousel__btn--next`);
        expect(rightButton).toBeDisplayed();

        // Check that the 3rd product is not yet displayed in viewport
        // Expect's toBeDisplayedInViewport and all similar functions do not work so we use the 'x' coordinates
        let product = $(`${productcarousel_selector} .product__card[data-product-sku="MH01-XS-Orange"]`);

        // Verify that the product is NOT displayed: it''s positioned at the right of the arrow
        expect(product.getLocation('x') < rightButton.getLocation('x')).toBe(false);

        // Click right/next arrow
        rightButton.click();
        browser.pause(2000); // wait until product "scroll" is done in carousel

        // Verify that the product is displayed: it''s now positioned at the left of the arrow
        expect(product.getLocation('x') < rightButton.getLocation('x')).toBe(true);
    });

    it('exposes the SKU of the products', () => {
        // Go to the product carousel page
        browser.url(productcarousel_page);
        let productCards = $$(`${productcarousel_selector} .product__card`);

        productCards.forEach((card) => {
            expect(card).toHaveAttribute('data-product-sku');
        });
    });
});
