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
const { logSpecStep } = require('../../lib/wdio.diagnostics');

const SPEC = 'product';

describe('Product component in CIF components library', () => {
    const product_page = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/product/sample-product.html/chaz-kangeroo-hoodie.html`;
    const product_selector = '.cmp-examples-demo__top .product';

    before(() => {
        logSpecStep(SPEC, 'before: AEMForceLogout + login + configureExamplesGraphqlClient (start)');
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Setup GraphQL client
        commons.configureExamplesGraphqlClient(browser);
        logSpecStep(SPEC, `before: done baseUrl=${browser.getUrl()}`);
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
        logSpecStep(SPEC, 'beforeEach: setWindowSize 1280x960');
    });

    it('can select a variant', () => {
        logSpecStep(SPEC, `it can select a variant: navigate ${product_page}`);
        // Go to the product page
        browser.url(product_page);
        logSpecStep(SPEC, `it can select a variant: after url=${browser.getUrl()}`);

        // Check that the grey variant color selection is displayed
        const greyColorButton = $(`${product_selector} button.tile__root[data-id="NTI="]`);
        const largeSizeButton = $(`${product_selector} button.tile__root[data-id="MTcy"]`);

        logSpecStep(SPEC, 'it can select a variant: waitForDisplayed grey NTI= + large MTcy (timeout 90000)');
        greyColorButton.waitForDisplayed({ timeout: 90000 });
        largeSizeButton.waitForDisplayed({ timeout: 90000 });
        logSpecStep(SPEC, 'it can select a variant: variant tiles visible');

        greyColorButton.click();
        largeSizeButton.click();
        logSpecStep(SPEC, 'it can select a variant: clicked grey + large');

        const productName = $(`${product_selector} .productFullDetail__productName > span`);
        logSpecStep(SPEC, 'it can select a variant: waitUntil product name Chaz Kangeroo Hoodie-L-Gray');
        browser.waitUntil(() => {
            if (!productName.isExisting()) {
                return false;
            }
            return productName.getText() === 'Chaz Kangeroo Hoodie-L-Gray';
        }, {
            timeout: 30000,
            interval: 200,
            timeoutMsg: 'Product name did not update after variant selection'
        });

        expect(productName).toHaveText('Chaz Kangeroo Hoodie-L-Gray');
        logSpecStep(SPEC, 'it can select a variant: done (assertions passed)');
    });

    it('exposes the SKU of the product', () => {
        logSpecStep(SPEC, `it exposes SKU: navigate ${product_page}`);
        // Go to the product page
        browser.url(product_page);
        logSpecStep(SPEC, `it exposes SKU: after url=${browser.getUrl()}`);

        logSpecStep(SPEC, 'it exposes SKU: waitUntil data-product-sku on .productFullDetail__root (timeout 90000)');
        browser.waitUntil(
            () => {
                const el = $(`${product_selector} .productFullDetail__root`);
                if (!el.isExisting()) {
                    return false;
                }
                const sku = el.getAttribute('data-product-sku');
                return sku != null && String(sku).length > 0;
            },
            {
                timeout: 90000,
                interval: 200,
                timeoutMsg: 'Product detail root did not expose data-product-sku after GraphQL load'
            }
        );

        const fullDetailElement = $(`${product_selector} .productFullDetail__root`);
        expect(fullDetailElement).toHaveAttribute('data-product-sku');
        logSpecStep(SPEC, 'it exposes SKU: done (assertions passed)');
    });
});
