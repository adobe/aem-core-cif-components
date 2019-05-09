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

window.CIF = window.CIF || {};

(function () {

    function PageContext() {
        let pageMask = document.querySelector("button.mask__root");

        return {
            maskPage: function() {
                pageMask.classList.toggle("mask__root_active");
            },
            unmaskPage: function() {
                pageMask.classList.toggle("mask__root_active");
            }
        }
    }

    function onDocumentReady() {
        window.CIF.PageContext = new PageContext();
    }

    if (document.readyState !== "loading") {
        onDocumentReady()
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

    

})();