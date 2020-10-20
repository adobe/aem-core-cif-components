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

(function() {
    const selectors = {
        self: "[data-comp-is='productcarousel']",
        btnPrev: "[data-carousel-action='prev']",
        btnNext: "[data-carousel-action='next']",
        container: '.productcarousel__cardscontainer',
        root: '.productcarousel__root',
        parent: '.productcarousel__parent',
        card: '.product__card'
    };

    function onDocumentReady() {
        const productCmp = document.querySelectorAll(selectors.self);
        if (productCmp) {
            productCmp.forEach(function(element) {
                new Carousel(element, selectors);
            });
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();
