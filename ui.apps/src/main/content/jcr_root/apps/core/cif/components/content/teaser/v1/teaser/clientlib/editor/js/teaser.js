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

($ => {
    'use strict';

    const dialogContentSelector = '[data-cmp-is="commerceteaser-editor"].cmp-teaser__editor';
    const productFieldSelector = '[data-cmp-teaser-v1-dialog-edit-hook="actionLink"][placeholder="Product"]';
    const categoryFieldSelector = '[data-cmp-teaser-v1-dialog-edit-hook="actionLink"][placeholder="Category"]';
    const actionsMultifieldSelector = '.cmp-teaser__editor-multifield_actions';
    const actionsMultifieldItemSelector = '.coral3-Multifield-item';
    const actionsEnabledCheckboxSelector = 'coral-checkbox[name="./actionsEnabled"]';

    // Listening for dialog windows to open
    // The config inputs are available only when the right dialog opens
    $(document).on('dialog-loaded', e => {
        const $dialog = e.dialog;

        // Checking if the dialog has the right selector for the teaser
        const $dialogContent = $dialog.find(dialogContentSelector);
        const dialogContent = $dialogContent.length > 0 ? $dialogContent[0] : undefined;

        // check if the expected dialog window was loaded
        if (dialogContent) {
            const multiFieldActions = $dialogContent.find(actionsMultifieldSelector)[0];

            // Handle pickers values change for existing teaser actions
            handlePickersChange(multiFieldActions);

            // The user can add and delete actions on the teaser
            // any time the user does this, event handlers need to be attached/reattached
            multiFieldActions.on('change', () => {
                // whenever actions are being added/removed, reattach picker change handlers
                handlePickersChange(multiFieldActions);
            });

            // Fix Core WCM Components Teaser editor bug.
            // when actions are disabled, only products picker gets disabled ( Core WCM Components Teaser expects only one action )
            // this also enables/disables the category picker
            const $actionsEnabledCheckbox = $dialogContent.find(actionsEnabledCheckboxSelector);
            $actionsEnabledCheckbox.on('change', e => {
                const actionsEnabled =
                    $(e.target)
                        .adaptTo('foundation-field')
                        .getValue() === 'true';
                $(categoryFieldSelector).each((ix, catEl) => {
                    $(catEl)
                        .adaptTo('foundation-field')
                        .setDisabled(!actionsEnabled);
                });
            });
        }
    });

    // used to handle picker value changes and keep only one picker populated
    const handlePickersChange = multiFieldActions => {
        // retrieve all teaser actions
        $(multiFieldActions)
            .find(actionsMultifieldItemSelector)
            .each((ix, action) => {
                // each action contains a category and a product picker

                // retrieve DOM elements for pickers
                const productElement = $(action).find(productFieldSelector);
                const categoryElement = $(action).find(categoryFieldSelector);

                // adapt the pickers so we can read/update values
                const productField = productElement.adaptTo('foundation-field');
                const categoryField = categoryElement.adaptTo('foundation-field');

                // remove attached handlers (if any)
                productElement.off('change', handleProductChange);
                categoryElement.off('change', handleCategoryChange);

                // create additional data to be sent to event handlers
                // this contains the Granite UI adapted fields
                const eventData = { productField, categoryField };

                // [re]attach change handlers with additional data
                productElement.on('change', eventData, handleProductChange);
                categoryElement.on('change', eventData, handleCategoryChange);
            });
    };

    // sets an empty value on the category field when product field gets updated
    const handleProductChange = ({ data: { productField, categoryField } }) => {
        if (productField.getValue() !== '') {
            categoryField.setValue('');
        }
    };

    // sets an empty value on the product field when category field gets updated
    const handleCategoryChange = ({ data: { productField, categoryField } }) => {
        if (categoryField.getValue() !== '') {
            productField.setValue('');
        }
    };
})(jQuery);
