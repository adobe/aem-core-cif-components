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

describe('Storybook in CIF components library', () => {

    const cart_with_items_page = `${config.aem.author.base_url}/etc.clientlibs/cif-components-examples/clientlibs/storybook/resources/index.html?path=/docs/commerce-cart--with-items`;

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

    it('basic react/clientlib check', () => {
        // Go to the cart page
        browser.url(cart_with_items_page);

        browser.pause(5000);

        const storybook = $('#storybook-preview-iframe');
        browser.switchToFrame(storybook);

        // Check that cart component is displayed
        const cartComponent = $('#story--commerce-cart--with-items .cmp-Minicart__minicart__root');
        expect(cartComponent).toBeDisplayed();
    });

});