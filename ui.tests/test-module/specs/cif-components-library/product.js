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

describe('Product component in CIF components library', () => {
    const product_page = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html`;
    const product_selector = '.cmp-examples-demo__top .product';

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

    it('can select a variant', () => {
        // Go to the product page
        browser.url(product_page);

        // Check that the grey variant color selection is displayed
        const greyColorButton = $(`${product_selector} button.tile__root[data-id="NTI="]`);
        expect(greyColorButton).toBeDisplayed();

        // Check that the L variant size selection is displayed
        const largeSizeButton = $(`${product_selector} button.tile__root[data-id="MTcy"]`);
        expect(largeSizeButton).toBeDisplayed();

        // Select grey and size L
        greyColorButton.click();
        largeSizeButton.click();

        // Verify that the product name was updated
        const productName = $(`${product_selector} .productFullDetail__productName > span`);
        expect(productName).toHaveText('Chaz Kangeroo Hoodie-L-Gray');
    });

    it('exposes the SKU of the product', () => {
        // Go to the product page
        browser.url(product_page);
        const fullDetailElement = $(`${product_selector} .productFullDetail__root`);

        expect(fullDetailElement).toHaveAttribute('data-product-sku');
    });
});
