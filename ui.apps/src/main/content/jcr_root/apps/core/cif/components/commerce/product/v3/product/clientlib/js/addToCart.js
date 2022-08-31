/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
'use strict';

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
        let parentSku = config.product.dataset.productSku;
        let productId = config.product.id;
        let sku = !configurable ? config.product.dataset.productSku : null;
        const useUid = config.product.hasAttribute('data-uid-cart');

        this._state = {
            sku,
            parentSku,
            productId,
            attributes: {},
            storefrontData: {},
            configurable,
            virtual,
            grouped,
            useUid
        };

        // Disable add to cart if configurable product and no variant was selected
        if (this._state.configurable && !this._state.sku) {
            this._element.disabled = true;
        }

        // prefill the storefrontData if available
        if (config.product.dataset.cifProductContext) {
            try {
                const contextData = JSON.parse(config.product.dataset.cifProductContext);
                this._state.storefrontData.name = contextData.name;
                this._state.storefrontData.regularPrice = contextData.pricing.regularPrice;
                this._state.storefrontData.finalPrice = contextData.pricing.specialPrice;
                this._state.storefrontData.currencyCode = contextData.pricing.currencyCode;
            } catch (e) {
                // ignore any parsing errors or incomplete data
            }
        }

        if (grouped) {
            // init
            this._onQuantityChanged();

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
        const { variant, attributes, selections } = event.detail;

        // Disable add to cart button if no valid variant is available
        if (!variant) {
            this._element.disabled = true;
            return;
        }

        // Update internal state
        this._state.sku = variant.sku;
        this._state.productId = variant.id;
        this._state.attributes = attributes;
        this._element.disabled = false;
        this._state.storefrontData = {
            sku: variant.sku,
            name: variant.name,
            regularPrice: variant.priceRange.regularPrice,
            currencyCode: variant.priceRange.currency,
            finalPrice: variant.priceRange.finalPrice,
            selectedOptions: Object.entries(selections).map(([attribute, value]) => ({
                attribute,
                value
            }))
        };
    }

    _onQuantityChanged() {
        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity));
        const item = selections.find(selection => parseInt(selection.value) > 0);
        this._element.disabled = item == null || !this._state.sku;
    }

    /**
     * Click event handler for add to cart button.
     */
    _onAddToCart(event) {
        const target = event.currentTarget;
        const items = this._getEventDetail();
        if (items.length > 0 && window.CIF) {
            const customEvent = new CustomEvent(AddToCart.events.addToCart, {
                bubbles: true,
                detail: items
            });
            target.dispatchEvent(customEvent);
            event.preventDefault();
            event.stopPropagation();
        }
    }

    _getEventDetail() {
        if (!this._state.sku) {
            return [];
        }

        if (this._state.grouped) {
            // To support grouped products where multiple products can be put in the cart in one single click,
            // the sku of each product is now read from the 'data-product-sku' attribute of each select element
            const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity)).filter(selection => {
                return parseInt(selection.value) > 0;
            });
            return selections.map(selection => {
                const item = {
                    productId: selection.dataset.productId,
                    sku: selection.dataset.productSku,
                    virtual: this._state.grouped ? selection.dataset.virtual !== undefined : this._state.virtual,
                    quantity: selection.value,
                    storefrontData: {
                        ...this._state.storefrontData,
                        quantity: selection.value
                    }
                };

                if (this._state.useUid) {
                    item.useUid = true;
                    item.parentSku = this._state.grouped ? item.sku : this._state.parentSku;
                    item.selected_options = Object.values(this._state.attributes);
                }

                return item;
            });
        } else {
            let quantity = 1;
            const quantityEl = document.querySelector(AddToCart.selectors.quantity);
            if (quantityEl) {
                quantity = parseInt(quantityEl.value);
            }

            const item = {
                productId: this._state.productId,
                sku: this._state.sku,
                virtual: this._state.virtual,
                quantity: quantity,
                storefrontData: {
                    ...this._state.storefrontData,
                    quantity: quantity
                }
            };

            if (this._state.useUid) {
                item.useUid = true;
                item.parentSku = this._state.parentSku;
                item.selected_options = Object.values(this._state.attributes);
            }

            return [item];
        }
    }
}

AddToCart.selectors = {
    self: '[data-cmp-is=add-to-cart]',
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
