/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

class ProductListConfig {
    constructor(jQuery) {
        this.$ = jQuery;
        this.handleDialogLoaded = this.handleDialogLoaded.bind(this);
        this.setFieldsDisabled = this.setFieldsDisabled.bind(this);
        this.actionsToggleHandler = this.actionsToggleHandler.bind(this);

        // Listening for dialog windows to open
        // The config inputs are available only when the right dialog opens
        this.$(document).on('dialog-loaded', this.handleDialogLoaded);
    }

    handleDialogLoaded(e) {
        const dialog = e.dialog[0];

        // Checking if the dialog has the right selector for the teaser
        const dialogContent = dialog.querySelector(ProductListConfig.selectors.dialogContentSelector);
        if (dialogContent) {

            const xfEnabledCheckbox = dialogContent.querySelector(
                ProductListConfig.selectors.xfEnabledCheckbox
            );
            this.actionsToggleHandler(xfEnabledCheckbox);
            const actionsEnabled = xfEnabledCheckbox.checked === true;
            this.setFieldsDisabled(!actionsEnabled);
        }
    }

    actionsToggleHandler(xfEnabledCheckbox) {
        xfEnabledCheckbox.addEventListener('change', e => {
            const actionsEnabled = e.target.checked === true;
            this.setFieldsDisabled(!actionsEnabled);
        });
    }

    setFieldsDisabled(disabled) {
        this.$(document.querySelector(ProductListConfig.selectors.xfSettings))
            .adaptTo('foundation-field')
            .setDisabled(disabled);

        document.querySelectorAll(ProductListConfig.selectors.xfLocation).forEach(el => {
            this.$(el)
                .adaptTo('foundation-field')
                .setDisabled(disabled);
        });

        document.querySelectorAll(ProductListConfig.selectors.xfPosition).forEach(el => {
            this.$(el)
                .adaptTo('foundation-field')
                .setDisabled(disabled);
        });
    }
}

ProductListConfig.selectors = {
    dialogContentSelector: '[data-cmp-is="productList-editor"].cmp-productList__editor',
    xfEnabledCheckbox: 'coral-checkbox[name="./fragmentEnabled"]',
    xfSettings: '.cmp-productList__editor-xf_settings',
    xfLocation: '[name="fragmentLocation"]',
    xfPosition: 'coral-numberinput[name="fragmentPosition"]'
};

(function(jQuery) {
    function onDocumentReady() {
        if (jQuery) {
            // Initialize ProductListConfig component
            new ProductListConfig(jQuery);
        }
    }

    console.log('ProductListConfig');

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})(window.jQuery);

export default ProductListConfig;
