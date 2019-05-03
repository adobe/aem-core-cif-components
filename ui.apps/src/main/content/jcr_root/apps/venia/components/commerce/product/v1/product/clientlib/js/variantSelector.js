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
        self: ".productFullDetail__options",
        variantButtons: ".productFullDetail__options button"
    };

    const events = {
        variantChanged: "variantchanged"
    };

    const buttonCheckIcon = '<span class="icon__root"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></span>';

    /**
     * Variant selector component.
     */
    function VariantSelector(config) {
        this._element = config.element;

        this._state = {
            // Currently selected variant
            variant: {},

            // Currently selected variant attributes
            attributes: {},

            // Reference to buttons
            buttons: [],

            // List of product variants
            variantData: []
        };

        // Parse variant data
        this._state.variantData = JSON.parse(this._element.dataset.variants);

        // Add click event handlers to variant selection buttons
        this._state.buttons = this._element.querySelectorAll(selectors.variantButtons);
        this._state.buttons.forEach(function(button) {
            button.addEventListener("click", this._onSelectVariant.bind(this));
        }, this);

        // Update button state on variant change
        this._element.addEventListener(events.variantChanged, this._updateButtonActiveClass.bind(this));
    }

    /**
     * Variant changed event handler that adds or removes active styles on variant
     * selection buttons based on the internal state.
     */
    VariantSelector.prototype._updateButtonActiveClass = function() {
        this._state.buttons.forEach(function(button) {
            const attributeIdentifier = button.closest("div.tileList__root").dataset.id;
            const valueIdentifier = button.dataset.id;

            if (this._state.attributes[attributeIdentifier] && this._state.attributes[attributeIdentifier] === valueIdentifier) {
                if (button.classList.contains("swatch__root")) {
                    button.classList.add("swatch__root_selected");
                    button.innerHTML = buttonCheckIcon; // Add check icon
                } else {
                    button.classList.add("tile__root_selected");
                }
            } else {
                if (button.classList.contains("swatch__root")) {
                    button.classList.remove("swatch__root_selected");
                    button.innerHTML = '';
                } else {
                    button.classList.remove("tile__root_selected");
                }
            }
        }, this);
    };

    /**
     * Returns the currently selected variant that matches the current selection stored
     * in the internal state or null if no matching variant exists.
     */
    VariantSelector.prototype._findSelectedVariant = function() {
        // Iterate variants
        for (let variant of this._state.variantData) {
            let match = true;

            // Iterate variant attributes
            for (let key in variant.variantAttributes) {
                let selectedValue = this._state.attributes[key];
                match = match && selectedValue == variant.variantAttributes[key];
            }

            // Return variant if all variant attributes match
            if (match) return variant;
        }

        // Return null if nothing matches
        return null;
    };

    /**
     * Click event handler for a variant selection button that selects a new
     * variant and stores it in the internal state and dispatches a variantchanged
     * event.
     */
    VariantSelector.prototype._onSelectVariant = function(event) {
        // Get value identifier from button
        const button = event.target.closest("button");
        const valueIdentifier = button.dataset.id;

        // Get attribute identifier from parent
        const parent = button.closest("div.tileList__root");
        const attributeIdentifier = parent.dataset.id;

        // Store selected variant
        this._state.attributes[attributeIdentifier] = valueIdentifier;

        // Find selected variant based on selected variants attributes
        this._state.variant = this._findSelectedVariant.bind(this)();

        // Emit variant change event
        let variantEvent = new CustomEvent(events.variantChanged, { bubbles: true, detail: this._state });
        this._element.dispatchEvent(variantEvent);

        // Don't reload page on click
        event.preventDefault();
    };

    function onDocumentReady() {
        // Initialize variant selector
        const variantSelectorCmp = document.querySelector(selectors.self);
        if (variantSelectorCmp) new VariantSelector({ element: variantSelectorCmp });
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})();