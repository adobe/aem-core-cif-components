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

const SPEC = 'product-teaser';

describe('Product Teaser component in the CIF components library', () => {
    const productTeaserPage = `${config.aem.author.base_url}/content/core-components-examples/library/commerce/productteaser.html`;
    const productTeaserSelector = '.cmp-examples-demo__top .productteaser';

    before(() => {
        logSpecStep(SPEC, 'before: AEM login + configureExamplesGraphqlClient (start)');
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Setup GraphQL client
        commons.configureExamplesGraphqlClient(browser);
        logSpecStep(SPEC, `before: done url=${browser.getUrl()}`);
    });

    it('exposes the SKU of the product', () => {
        logSpecStep(SPEC, `it teaser SKU: navigate ${productTeaserPage}`);
        // Go to the product page
        browser.url(productTeaserPage);
        logSpecStep(SPEC, `it teaser SKU: after url=${browser.getUrl()}`);

        // check the element for the data-product-sku attribute
        logSpecStep(SPEC, 'it teaser SKU: expect .item__root data-product-sku');
        const productTeaserElement = $(`${productTeaserSelector} .item__root`);
        expect(productTeaserElement).toHaveAttribute('data-product-sku');
        logSpecStep(SPEC, 'it teaser SKU: done');
    });
});
