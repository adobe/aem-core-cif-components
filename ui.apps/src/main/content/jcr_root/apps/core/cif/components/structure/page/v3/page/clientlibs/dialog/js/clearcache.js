/*******************************************************************************
 *
 *    Copyright 2024 Adobe. All rights reserved.
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

(function(document, $) {
    'use strict';
    const DIALOG_ID = 'cif-confirm-clear-cache-dialog';
    const CLEAR_CACHE_BUTTON_SELECTOR = '.cif-clear-cache-button';
    const CLEAR_CACHE_INPUT_SELECTOR = 'cif-clear-cache-input';

    $(document).ready(function() {
        // Select the button by class
        $(CLEAR_CACHE_BUTTON_SELECTOR).click(function(e) {
            e.preventDefault();

            // Show confirmation dialog
            showConfirmationDialog($(this));
        });

        function showConfirmationDialog(button) {
            // Check if the dialog already exists
            let dialog = document.getElementById(DIALOG_ID);
            if (!dialog) {
                dialog = new Coral.Dialog().set({
                    id: DIALOG_ID,
                    header: {
                        innerHTML: Granite.I18n.get('Confirm Clear Cache')
                    },
                    content: {
                        innerHTML: `<p>${Granite.I18n.get('Are you sure you want to clear the commerce cache?')}</p>`
                    },
                    footer: {
                        innerHTML:
                            `<button is="coral-button" variant="primary" coral-close id="${CLEAR_CACHE_INPUT_SELECTOR}">` +
                            Granite.I18n.get('Yes') +
                            '</button>' +
                            '<button is="coral-button" variant="default" coral-close>' +
                            Granite.I18n.get('No') +
                            '</button>'
                    }
                });
                document.body.appendChild(dialog);
            }
            dialog.show();

            // Handle confirmation
            $("#" + CLEAR_CACHE_INPUT_SELECTOR).off('click').on('click', function() {
                clearCache(button);
            });
        }

        function clearCache(button) {
            // Get the action and method from data attributes
            let actionUrl = button.data('action');
            let method = button.data('method');
            // Get the value of storePath from the input field
            let storePath = $(button.data('storepath')).val();

            // Perform an AJAX call using jQuery
            $.ajax({
                type: method,
                url: actionUrl,
                contentType: 'application/json',
                data: JSON.stringify({
                    storePath: storePath
                }),
                success: function(response) {
                    showDialog(Granite.I18n.get('Cache cleared successfully!'));
                },
                error: function() {
                    showDialog(Granite.I18n.get('Failed to clear cache!'));
                }
            });
        }

        function showDialog(message) {
            let dialog = new Coral.Dialog().set({
                id: 'clear-cache-dialog',
                header: {
                    innerHTML: Granite.I18n.get('Clear Cache')
                },
                content: {
                    innerHTML: `<p>${message}</p>`
                },
                footer: {
                    innerHTML:
                        '<button is="coral-button" variant="primary" coral-close>' +
                        Granite.I18n.get('OK') +
                        '</button>'
                }
            });
            document.body.appendChild(dialog);
            dialog.show();
        }
    });
})(document, Granite.$);
