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

(function(){

    class MiniCart {
        constructor() {

            this.rootNode = document.querySelector(".miniCart__root");

            // just trigger an event to let other components know we're ready.
            const event = new CustomEvent("aem.cif.cart-intialized");
            document.dispatchEvent(event);

            this._initializeBehavior();
            this.pageContext = window.CIF.PageContext;
        }

        _initializeBehavior() {
            let closeButton = this.rootNode.querySelector("button[data-action='close']");
            closeButton.addEventListener('click', event => {
                this.close();
            })

        }
        
        open() {
            this.rootNode.classList.add("miniCart__root_open");
            this.pageContext.maskPage();
        }

        close() {
            this.rootNode.classList.remove("miniCart__root_open");
            this.pageContext.unmaskPage();
        }
    }

    function onDocumentReady() {
        window.CIF.MiniCart = new MiniCart();
    }

    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})();