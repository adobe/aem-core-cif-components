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

/**
 * Variant selector component.
 */
class VariantSelector {
    constructor(config) {
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
        this._state.buttons = this._element.querySelectorAll(VariantSelector.selectors.variantButtons);
        this._state.buttons.forEach(function(button) {
            button.addEventListener('click', this._onSelectVariant.bind(this));
        }, this);

        // Update button state on variant change
        this._element.addEventListener(VariantSelector.events.variantChanged, this._updateButtonActiveClass.bind(this));

        // Check for initial variant sku in hash and listen for url changes
        this._initFromHash();
        window.addEventListener('popstate', this._initFromHash.bind(this));
    }

    /**
     * Select variant from the current location hash. This method will update
     * the internal state and emit a variantchanged event, so all components can
     * update accordingly.
     */
    _initFromHash() {
        let sku = window.location.hash.replace('#', '');
        if (!sku) return;

        // Find variant with given sku
        this._state.variant = this._findSelectedVariant(sku);

        // If variant is valid, store variant attributes and emit event to update
        // buttons and parent components.
        if (this._state.variant) {
            this._state.attributes = { ...this._state.variant.variantAttributes };
            this._emitVariantChangedEvent();
        }
    }

    /**
     * Emit a variantchanged event with a copy of the current component state.
     */
    _emitVariantChangedEvent() {
        let variantEvent = new CustomEvent(VariantSelector.events.variantChanged, {
            bubbles: true,
            detail: this._state
        });
        this._element.dispatchEvent(variantEvent);
    }

    /**
     * Variant changed event handler that adds or removes active styles on variant
     * selection buttons based on the internal state.
     */
    _updateButtonActiveClass() {
        this._state.buttons.forEach(function(button) {
            const attributeIdentifier = button.closest('div.tileList__root').dataset.id;
            const valueIdentifier = button.dataset.id;

            if (
                this._state.attributes[attributeIdentifier] &&
                this._state.attributes[attributeIdentifier] == valueIdentifier
            ) {
                if (button.classList.contains('swatch__root')) {
                    button.classList.add('swatch__root_selected');
                    button.innerHTML = VariantSelector.buttonCheckIcon; // Add check icon
                } else {
                    button.classList.add('tile__root_selected');
                }
            } else {
                if (button.classList.contains('swatch__root')) {
                    button.classList.remove('swatch__root_selected');
                    button.innerHTML = '';
                } else {
                    button.classList.remove('tile__root_selected');
                }
            }
        }, this);
    }

    /**
     * Returns the currently selected variant that matches either the sku given
     * as argument or the current variant attribute selection stored in the
     * internal state or null if no matching variant exists.
     */
    _findSelectedVariant(sku) {
        // Iterate variants
        for (let variant of this._state.variantData) {
            let match = true;

            // If sku is given, match with sku
            if (sku && variant.sku == sku) {
                return variant;
            }

            // Iterate variant attributes
            if (sku) continue;
            for (let key in variant.variantAttributes) {
                let selectedValue = this._state.attributes[key];
                match = match && selectedValue == variant.variantAttributes[key];
            }

            // Return variant if all variant attributes match
            if (match) return variant;
        }

        // Return null if nothing matches
        return null;
    }

    /**
     * Click event handler for a variant selection button that selects a new
     * variant and stores it in the internal state and dispatches a variantchanged
     * event.
     */
    _onSelectVariant(event) {
        // Get value identifier from button
        const button = event.target.closest('button');
        const valueIdentifier = button.dataset.id;

        // Get attribute identifier from parent
        const parent = button.closest('div.tileList__root');
        const attributeIdentifier = parent.dataset.id;

        // Store selected variant
        this._state.attributes[attributeIdentifier] = valueIdentifier;

        // Find selected variant based on selected variants attributes
        this._state.variant = this._findSelectedVariant.bind(this)();
        this._emitVariantChangedEvent();

        // Update hash only if a proper variant was found
        if (this._state.variant) {
            this._setHash(this._state.variant.sku);
        }

        // Don't reload page on click
        event.preventDefault();
    }

    /**
     * Set location hash by pushing to the history state.
     */
    _setHash(sku) {
        history.pushState(null, null, '#' + sku);
    }
}

VariantSelector.selectors = {
    self: '.productFullDetail__options',
    variantButtons: '.productFullDetail__options button'
};

VariantSelector.events = {
    variantChanged: 'variantchanged'
};

VariantSelector.buttonCheckIcon =
    '<span class="icon__root"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></span>';

(function(document) {
    function onDocumentReady() {
        // Initialize variant selector
        const variantSelectorCmp = document.querySelector(VariantSelector.selectors.self);
        if (variantSelectorCmp) new VariantSelector({ element: variantSelectorCmp });
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.document);

export default VariantSelector;
