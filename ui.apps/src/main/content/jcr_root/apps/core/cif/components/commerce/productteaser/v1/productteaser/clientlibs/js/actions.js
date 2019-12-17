/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
'use strict';
(function(channel) {
    const addToCartHandler = ev => {
        const button = ev.currentTarget;
        const sku = button.dataset['itemSku'];
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: { sku, quantity: 1 }
        });
        document.dispatchEvent(customEvent);
    };

    const seeDetailsHandler = ev => {
        const button = ev.currentTarget;
        const url = button.dataset['url'];
        window.location = url;
    };

    const initializeTeaserAction = function() {
        console.log(`Let's initialize this!`);
        const actionButtons = channel.querySelectorAll('.productteaser__cta button');

        actionButtons.forEach(node => {
            const action = node.dataset['action'];
            let actionHandler;
            if (action === 'addToCart') {
                actionHandler = addToCartHandler;
            } else if (action === 'details') {
                actionHandler = seeDetailsHandler;
            } else {
                actionHandler = () => {
                    /* NOOP */
                };
            }

            node.addEventListener('click', actionHandler);
        });
    };

    if (document.readyState !== 'loading') {
        initializeTeaserAction();
    } else {
        document.addEventListener('DOMContentLoaded', initializeTeaserAction);
    }
})(window.document);
