/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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

class TeaserConfig {
    constructor(jQuery) {
        this.$ = jQuery;
        this.handleDialogLoaded = this.handleDialogLoaded.bind(this);
        this.attachEventHandlers = this.attachEventHandlers.bind(this);
        this.setFieldsDisabled = this.setFieldsDisabled.bind(this);
        this.actionsToggleHandler = this.actionsToggleHandler.bind(this);
        this.handlePickersChange = this.handlePickersChange.bind(this);
        this.handlePageChange = this.handlePageChange.bind(this);
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
            const actionsEnabled = actionsEnabledCheckbox.checked === true;
            this.setFieldsDisabled(!actionsEnabled);
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
        actionsEnabledCheckbox.addEventListener('change', e => {
            const actionsEnabled = e.target.checked === true;
            this.setFieldsDisabled(!actionsEnabled);
        });
    }

    setFieldsDisabled(disabled) {
        document.querySelectorAll(TeaserConfig.selectors.categoryFieldSelector).forEach(catEl => {
            this.$(catEl)
                .adaptTo('foundation-field')
                .setDisabled(disabled);
        });
        document.querySelectorAll(TeaserConfig.selectors.productFieldSelector).forEach(prodEl => {
            this.$(prodEl)
                .adaptTo('foundation-field')
                .setDisabled(disabled);
        });
    }

    // used to handle picker value changes and keep only one picker populated
    handlePickersChange(multiFieldActions) {
        // retrieve all teaser actions
        multiFieldActions.querySelectorAll(TeaserConfig.selectors.actionsMultifieldItemSelector).forEach(action => {
            // each action contains a page picker, a category picker and a product picker
            // retrieve DOM elements for pickers
            const pageElement = this.$(action.querySelector(TeaserConfig.selectors.pageFieldSelector));
            const productElement = this.$(action.querySelector(TeaserConfig.selectors.productFieldSelector));
            const categoryElement = this.$(action.querySelector(TeaserConfig.selectors.categoryFieldSelector));

            // adapt the pickers so we can read/update values
            const pageField = pageElement.adaptTo('foundation-field');
            const productField = productElement.adaptTo('foundation-field');
            const categoryField = categoryElement.adaptTo('foundation-field');

            // remove attached handlers (if any)
            pageElement.off('change', this.handlePageChange);
            productElement.off('change', this.handleProductChange);
            categoryElement.off('change', this.handleCategoryChange);

            // create additional data to be sent to event handlers
            // this contains the Granite UI adapted fields
            const eventData = { pageField, productField, categoryField };

            // [re]attach change handlers with additional data
            pageElement.on('change', eventData, this.handlePageChange);
            productElement.on('change', eventData, this.handleProductChange);
            categoryElement.on('change', eventData, this.handleCategoryChange);
        });
    }

    // sets an empty value on the category field when product field gets updated
    handlePageChange({ data: { pageField, productField, categoryField } }) {
        if (pageField.getValue() !== '') {
            productField.setValue('');
            categoryField.setValue('');
        }
    }

    // sets an empty value on the category field when product field gets updated
    handleProductChange({ data: { pageField, productField, categoryField } }) {
        if (productField.getValue() !== '') {
            pageField.setValue('');
            categoryField.setValue('');
        }
    }

    // sets an empty value on the product field when category field gets updated
    handleCategoryChange({ data: { pageField, productField, categoryField } }) {
        if (categoryField.getValue() !== '') {
            pageField.setValue('');
            productField.setValue('');
        }
    }
}

TeaserConfig.selectors = {
    dialogContentSelector: '[data-cmp-is="commerceteaser-editor"].cmp-teaser__editor',
    pageFieldSelector: '[data-cmp-teaser-v1-dialog-edit-hook="actionLink"]',
    productFieldSelector: '[data-cmp-teaser-v1-dialog-edit-hook="actionProduct"]',
    categoryFieldSelector: '[data-cmp-teaser-v1-dialog-edit-hook="actionCategory"]',
    actionsMultifieldSelector: '.cmp-teaser__editor-multifield_actions',
    actionsMultifieldItemSelector: 'coral-multifield-item',
    actionsEnabledCheckboxSelector: 'coral-checkbox[name="./actionsEnabled"]'
};

(function(jQuery) {
    function onDocumentReady() {
        if (jQuery) {
            // Initialize TeaserConfig component
            new TeaserConfig(jQuery);
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.jQuery);

export default TeaserConfig;
