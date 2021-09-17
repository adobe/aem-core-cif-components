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

    get _element() {
        // always query the add to cart button, as it may be replaced by react component when the storefront is integrated with peregrine
        // see the addToCart react component
        return document.querySelector(AddToCart.selectors.self);
    }

    constructor(config) {
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


        this._onQuantityChanged(); // init
        // Disable/enable add to cart based on the selected quantities of a grouped product
        document.querySelectorAll(AddToCart.selectors.quantity).forEach(selection => {
            selection.addEventListener('change', this._onQuantityChanged.bind(this));
        });

        // Listen to variant updates on product
        config.product.addEventListener(AddToCart.events.variantChanged, this._onUpdateVariant.bind(this));

        // Add click handler to add to cart button
        this._element.addEventListener('click', this._onAddToCart.bind(this));

        this._dispatchStateChangeToButton();
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
        this._dispatchStateChangeToButton();
    }

    _onQuantityChanged() {
        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity));
        let item = selections.find(selection => {
            return parseInt(selection.value) > 0;
        });
        this._element.disabled = item == null || !this._state.sku;
        this._dispatchStateChangeToButton();
    }

    /**
     * Click event handler for add to cart button.
     */
    _onAddToCart() {
        const items = this._getEventDetail();
        if (items.length > 0 && window.CIF) {
            const customEvent = new CustomEvent(AddToCart.events.addToCart, {
                detail: items
            });
            document.dispatchEvent(customEvent);
        }
    }

    _getEventDetail() {
        // To support grouped products where multiple products can be put in the cart in one single click,
        // the sku of each product is now read from the 'data-product-sku' attribute of each select element
        const selections = Array.from(document.querySelectorAll(AddToCart.selectors.quantity)).filter(selection => {
            return parseInt(selection.value) > 0;
        });
        const items = selections.map(selection => {
            return {
                productId: selection.dataset.productId,
                sku: selection.dataset.productSku,
                virtual: this._state.grouped ? selection.dataset.virtual !== undefined : this._state.virtual,
                quantity: selection.value
            };
        });

        return items;
    }

    /**
     * This method dispatches an event to the add to cart button (this._element) that contains the event detail in the same format the
     * add-to-cart event would provide it. This is used to update the state of the add to cart react component
     */
    _dispatchStateChangeToButton() {
        const items = this._getEventDetail();
        // update the data set of the host, when dispatchStateChangeToButton is called before the portal placer replaced the add to cart
        // button
        this._element.parentElement.setAttribute('data-items', JSON.stringify(items));
        // dispatch the custom event to the button
        this._element.dispatchEvent(
            new CustomEvent(AddToCart.events.stateChanged, {
                detail: items
            })
        );
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
    stateChanged: 'aem.cif.internal.add-to-cart.state-changed',
    addToCart: 'aem.cif.add-to-cart'
};

(function(document) {
    function onDocumentReady() {
        // Initialize AddToCart component
        const productCmp = document.querySelector(AddToCart.selectors.product);
        if (productCmp) {
            new AddToCart({ product: productCmp });
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default AddToCart;
