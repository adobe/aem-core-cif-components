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

const dataLayerEnabled = document.body.hasAttribute('data-cmp-data-layer-enabled');
const dataLayer = dataLayerEnabled ? (window.adobeDataLayer = window.adobeDataLayer || []) : undefined;

/**
 * Add to cart button component.
 */
class AddToCart {
    constructor(config) {
        this._element = config.element;

        // Get configuration from product reference
        let configurable = config.product.dataset.configurable !== undefined;
        let virtual = config.product.dataset.virtual !== undefined;
        let grouped = config.product.dataset.grouped !== undefined;
        let sku = !configurable ? config.product.querySelector(AddToCart.selectors.sku).innerHTML : null;

        this._state = {
            sku,
            attributes: {},
            configurable,
            virtual,
            grouped
        };

        // Disable add to cart if configurable product and no variant was selected
        if (this._state.configurable && !this._state.sku) {
            this._element.disabled = true;
        }

        if (grouped) {
            this._onQuantityChanged(); // init
            // Disable/enable add to cart based on the selected quantities of a grouped product
            document.querySelectorAll(AddToCart.selectors.quantity).forEach(selection => {
                selection.addEventListener('change', this._onQuantityChanged.bind(this));
            });
        }

        // Listen to variant updates on product
        config.product.addEventListener(AddToCart.events.variantChanged, this._onUpdateVariant.bind(this));

        // Add click handler to add to cart button
        this._element.addEventListener('click', this._onAddToCart.bind(this));
    }

    /**
     * Variant changed event handler.
     */
    _onUpdateVariant(event) {
        const variant = event.detail.variant;

        // Disable add to cart button if no valid variant is available
        if (!variant) {
            this._element.disabled = true;
            return;
        }

        // Update sku attribute in select element
        document.querySelector(AddToCart.selectors.quantity).setAttribute('data-product-sku', variant.sku);
        document.querySelector(AddToCart.selectors.quantity).setAttribute('data-product-id', variant.id);

        // Update internal state
        this._state.sku = variant.sku;
        this._state.attributes = event.detail.attributes;
        this._element.disabled = false;
    }

    _onQuantityChanged() {
        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity));
        let item = selections.find(selection => {
            return parseInt(selection.value) > 0;
        });
        this._element.disabled = item == null;
    }

    /**
     * Click event handler for add to cart button.
     */
    _onAddToCart() {
        // To support grouped products where multiple products can be put in the cart in one single click,
        // the sku of each product is now read from the 'data-product-sku' attribute of each select element

        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity)).filter(selection => {
            return parseInt(selection.value) > 0;
        });
        let items = selections.map(selection => {
            return {
                sku: selection.dataset.productSku,
                virtual: this._state.grouped ? selection.dataset.virtual !== undefined : this._state.virtual,
                quantity: selection.value
            };
        });

        if (items.length > 0 && window.CIF) {
            const customEvent = new CustomEvent(AddToCart.events.addToCart, {
                detail: items
            });
            document.dispatchEvent(customEvent);

            if (dataLayerEnabled) {
                selections.forEach(function(selection) {
                    // https://github.com/adobe/xdm/blob/master/docs/reference/datatypes/productlistitem.schema.md
                    dataLayer.push({
                        event: 'cif:addToCart',
                        eventInfo: {
                            '@id': selection.dataset.productId,
                            'xdm:SKU': selection.dataset.productSku,
                            'xdm:quantity': selection.value
                        }
                    });
                });
            }
        }
    }
}

AddToCart.selectors = {
    self: '.productFullDetail__cartActions button',
    sku: '.productFullDetail__details [role=sku]',
    quantity: '.productFullDetail__quantity select',
    product: '[data-cmp-is=product]'
};

AddToCart.events = {
    variantChanged: 'variantchanged',
    addToCart: 'aem.cif.add-to-cart'
};

(function(document) {
    function onDocumentReady() {
        // Initialize AddToCart component
        const productCmp = document.querySelector(AddToCart.selectors.product);
        const addToCartCmp = document.querySelector(AddToCart.selectors.self);
        if (addToCartCmp) new AddToCart({ element: addToCartCmp, product: productCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default AddToCart;
