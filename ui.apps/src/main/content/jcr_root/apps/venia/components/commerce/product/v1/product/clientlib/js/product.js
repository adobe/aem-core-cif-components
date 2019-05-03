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
    "use strict";

    const selectors = {
        self: "[data-cmp-is=product]",
        sku: ".productFullDetail__details [role=sku]",
        name: ".productFullDetail__title [role=name]",
        price: ".productFullDetail__productPrice [role=price]",
        description: ".productFullDetail__description [role=description]",
        mainImage: ".carousel__currentImage",
    };

    const events = {
        variantChanged: "variantchanged"
    };

    /**
     * Product component.
     */
    function Product(config) {
        this._element = config.element;

        // Local state
        this._state = {
            // Current sku, either from the base product or from a variant
            sku: null,

            // True if this product is configurable and has variants
            configurable: false
        };
        this._state.configurable = this._element.dataset.configurable !== undefined;
        this._state.sku = !this._state.configurable ? this._element.querySelector(selectors.sku).innerHTML : null;

        // Update product data
        this._element.addEventListener(events.variantChanged, this._onUpdateVariant.bind(this));
    }

    /**
     * Variant changed event handler that updates the displayed product attributes
     * based on the given event.
     */
    Product.prototype._onUpdateVariant = function(event) {
        const variant = event.detail.variant;
        if(!variant) return;

        // Update internal state
        this._state.sku = variant.sku;

        // Update values and enable add to cart button
        this._element.querySelector(selectors.sku).innerText = variant.sku;
        this._element.querySelector(selectors.name).innerText = variant.name;
        this._element.querySelector(selectors.price).innerText = variant.formattedPrice;
        this._element.querySelector(selectors.description).innerHTML = DOMPurify.sanitize(variant.description);

        // TODO: Update assets, this has a dependency on CIF-775
        // For demo only, the gallery component should handle this based on the incoming event
        if (variant.assets.length > 0) this._element.querySelector(selectors.mainImage).src = variant.assets[0].path;
    };

    function onDocumentReady() {
        // Initialize product component
        const productCmp = document.querySelector(selectors.self);
        if (productCmp) new Product({ element: productCmp });
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})();