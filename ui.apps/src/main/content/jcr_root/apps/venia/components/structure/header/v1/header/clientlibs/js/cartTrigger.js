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

(function() {
    const cartCounterDom = counter => `<span class="cartCounter__root">${counter}</span>`;

    class CartTrigger {
        constructor() {
            this.rootNode = document.querySelector('button.cartTrigger__root');
            this.rootNode.addEventListener('click', event => {
                if (window.CIF.MiniCart) {
                    window.CIF.MiniCart.open();
                }
            });

            this.count = 0;

            const qtyChangedEventHandler = ev => {
                console.log(`Receives cart event ${ev.detail}`);
                const data = ev.detail;
                this.updateCounterText(data.quantity);
            };

            document.addEventListener('aem.cif.product-added-to-cart', qtyChangedEventHandler);
            document.addEventListener('aem.cif.product-removed-from-cart', qtyChangedEventHandler);
            document.addEventListener('aem.cif.cart-intialized', qtyChangedEventHandler);
            document.addEventListener('aem.cif.product-cart-updated', qtyChangedEventHandler);
        }

        updateCounterText(count) {
            if (count == null || isNaN(count)) {
                return;
            }

            const counter = this.rootNode.querySelector('.cartCounter__root');
            if (counter) {
                this.rootNode.removeChild(counter);
            }
            if (count === 0) {
                return;
            }
            const toElement = count => {
                const html = new DOMParser().parseFromString(cartCounterDom(count), 'text/html');
                return html.body.firstChild;
            };

            this.rootNode.appendChild(toElement(count));
        }
    }

    function initializeCartTrigger() {
        window.CIF.CartTrigger = new CartTrigger();
    }

    if (document.readyState !== 'loading') {
        initializeCartTrigger();
    } else {
        document.addEventListener('DOMContentLoaded', initializeCartTrigger);
    }
})();
