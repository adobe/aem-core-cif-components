/*******************************************************************************
 * ADOBE CONFIDENTIAL
 * __________________
 *
 * Copyright 2019 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 ******************************************************************************/

(function () {

    const cartCounterDom = (counter) => (`<span class="cartCounter__root">${counter}</span>`);

    class CartTrigger {
        constructor() {
            this.rootNode = document.querySelector("button.cartTrigger__root");
            this.rootNode.addEventListener("click", event => {
                if (window.CIF.MiniCart) {
                    window.CIF.MiniCart.open();
                }
            });

            this.count = 0;
        }

        updateCounterText(count) {
            if (count == null || isNaN(count)) {
                return;
            }

            const counter = this.rootNode.querySelector(".cartCounter__root");
            if (counter) {
                this.rootNode.removeChild(counter);
            }
            if (count === 0) {
                return;
            }
            const toElement = (count) => {
                const html = new DOMParser().parseFromString(cartCounterDom(count), "text/html");
                return html.body.firstChild;
            };

            this.rootNode.appendChild(toElement(count));
        }

    }

    function initializeCartTrigger() {
        window.CIF.CartTrigger = new CartTrigger();

    }

    if (document.readyState !== "loading") {
        initializeCartTrigger()
    } else {
        document.addEventListener("DOMContentLoaded", initializeCartTrigger);
    }

})();