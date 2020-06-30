/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

class TeaserConfig {
    constructor(jQuery) {
        this.$ = jQuery;
        this.handleDialogLoaded = this.handleDialogLoaded.bind(this);
        this.attachEventHandlers = this.attachEventHandlers.bind(this);
        this.actionsToggleHandler = this.actionsToggleHandler.bind(this);
        this.handlePickersChange = this.handlePickersChange.bind(this);
        this.handleProductChange = this.handleProductChange.bind(this);
        this.handleCategoryChange = this.handleCategoryChange.bind(this);

        // Listening for dialog windows to open
        // The config inputs are available only when the right dialog opens
        this.$(document).on('dialog-loaded', this.handleDialogLoaded);
    }

    handleDialogLoaded(e) {
        const dialog = e.dialog[0];

        // Checking if the dialog has the right selector for the teaser
        const dialogContent = dialog.querySelector(TeaserConfig.selectors.dialogContentSelector);
        if (dialogContent) {
            const multiFieldActions = dialogContent.querySelector(TeaserConfig.selectors.actionsMultifieldSelector);
            this.attachEventHandlers(multiFieldActions);

            const actionsEnabledCheckbox = dialogContent.querySelector(
                TeaserConfig.selectors.actionsEnabledCheckboxSelector
            );
            this.actionsToggleHandler(actionsEnabledCheckbox);
        }
    }

    attachEventHandlers(multiFieldActions) {
        // Handle pickers values change for existing teaser actions
        this.handlePickersChange(multiFieldActions);

        // The user can add and delete actions on the teaser
        // any time the user does this, event handlers need to be attached/reattached
        this.$(multiFieldActions).on('change', () => {
            // whenever actions are being added/removed, reattach picker change handlers
            this.handlePickersChange(multiFieldActions);
        });
    }

    // Fix Core WCM Components Teaser editor bug.
    // when actions are disabled, only products picker gets disabled ( Core WCM Components Teaser expects only one action )
    // this also enables/disables the category picker
    actionsToggleHandler(actionsEnabledCheckbox) {
        this.$(actionsEnabledCheckbox).on('change', e => {
            const actionsEnabled =
                this.$(e.target)
                    .adaptTo('foundation-field')
                    .getValue() === 'true';
            document.querySelectorAll(TeaserConfig.selectors.categoryFieldSelector).forEach(catEl => {
                this.$(catEl)
                    .adaptTo('foundation-field')
                    .setDisabled(!actionsEnabled);
            });
        });
    }

    // used to handle picker value changes and keep only one picker populated
    handlePickersChange(multiFieldActions) {
        // retrieve all teaser actions
        multiFieldActions.querySelectorAll(TeaserConfig.selectors.actionsMultifieldItemSelector).forEach(action => {
            // each action contains a category and a product picker
            // retrieve DOM elements for pickers
            const productElement = this.$(action.querySelector(TeaserConfig.selectors.productFieldSelector));
            const categoryElement = this.$(action.querySelector(TeaserConfig.selectors.categoryFieldSelector));

            // adapt the pickers so we can read/update values
            const productField = productElement.adaptTo('foundation-field');
            const categoryField = categoryElement.adaptTo('foundation-field');

            // remove attached handlers (if any)
            productElement.off('change', this.handleProductChange);
            categoryElement.off('change', this.handleCategoryChange);

            // create additional data to be sent to event handlers
            // this contains the Granite UI adapted fields
            const eventData = { productField, categoryField };

            // [re]attach change handlers with additional data
            productElement.on('change', eventData, this.handleProductChange);
            categoryElement.on('change', eventData, this.handleCategoryChange);
        });
    }

    // sets an empty value on the category field when product field gets updated
    handleProductChange({ data: { productField, categoryField } }) {
        if (productField.getValue() !== '') {
            categoryField.setValue('');
        }
    }

    // sets an empty value on the product field when category field gets updated
    handleCategoryChange({ data: { productField, categoryField } }) {
        if (categoryField.getValue() !== '') {
            productField.setValue('');
        }
    }
}

TeaserConfig.selectors = {
    dialogContentSelector: '[data-cmp-is="commerceteaser-editor"].cmp-teaser__editor',
    productFieldSelector: '[data-cmp-teaser-v1-dialog-edit-hook="actionLink"][placeholder="Product"]',
    categoryFieldSelector: '[data-cmp-teaser-v1-dialog-edit-hook="actionLink"][placeholder="Category"]',
    actionsMultifieldSelector: '.cmp-teaser__editor-multifield_actions',
    actionsMultifieldItemSelector: 'coral-multifield-item',
    actionsEnabledCheckboxSelector: 'coral-checkbox[name="./actionsEnabled"]'
};

(function(jQuery) {
    function onDocumentReady() {
        // Initialize TeaserConfig component
        new TeaserConfig(jQuery);
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.jQuery);

export default TeaserConfig;
