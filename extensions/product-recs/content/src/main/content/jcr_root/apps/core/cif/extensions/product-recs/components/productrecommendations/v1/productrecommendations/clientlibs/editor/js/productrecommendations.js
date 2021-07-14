/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
(function($) {
    'use strict';

    var dialogContentSelector = '.cif-product-recs__editor';
    var enableFilterSelector = '[name="./usedFilter"],[name="./usedFilter@Delete"]';
    var usePredefinedSelector = '.cif-product-recs__use-predefined';
    var recsOptions = '.cif-product-recs__editor-options';

    $(document).on('dialog-loaded', function(e) {
        var $dialog = e.dialog;
        var $dialogContent = $dialog.find(dialogContentSelector);
        var dialogContent = $dialogContent.length > 0 ? $dialogContent[0] : undefined;

        if (dialogContent) {
            init($dialogContent);
        }
    });

    function enableFilter(target, enable, $dialogContent) {
        if (target.length === 0) return;
        if (target.data('targetId')) {
            var targetedBy = $dialogContent.find('[target="[data-target-id=\'' + target.data('targetId') + '\']"]');
            targetedBy.adaptTo('foundation-field').setDisabled(!enable);
        } else {
            target.adaptTo('foundation-field').setDisabled(!enable);
        }
    }

    function handleFilterSelection(target, enabled, $dialogContent) {
        if (target.indexOf('Range') > -1) {
            var minEl = $dialogContent.find('[name="' + target + 'Min' + '"]');
            enableFilter(minEl, enabled, $dialogContent);

            var maxEl = $dialogContent.find('[name="' + target + 'Max' + '"]');
            enableFilter(maxEl, enabled, $dialogContent);
        } else {
            var targetEl = $dialogContent.find('[name="' + target + '"]');
            enableFilter(targetEl, enabled, $dialogContent);
        }
    }

    function displayRecsOptions(hide, $dialogContent) {
        var optionsContainer = $dialogContent.find(recsOptions);
        if (hide) {
            $(optionsContainer).hide();
        } else {
            $(optionsContainer).show();
        }
    }

    function init($dialogContent) {
        var filters = $dialogContent.find(enableFilterSelector);

        filters.each(function(index, element) {
            var enabled = $(element).is(':checked');
            var target = element.value;

            handleFilterSelection(target, enabled, $dialogContent);

            $(element).on('change', function(e) {
                var enabled = $(e.target).is(':checked');
                var target = e.target.value;

                for (var i = 0; i < filters.length; i++) {
                    handleFilterSelection(filters[i].value, false, $dialogContent);
                }

                handleFilterSelection(target, enabled, $dialogContent);
            });
        });

        var usePreconfiguredElement = $dialogContent.find(usePredefinedSelector);
        var usePreconfigured =
            $(usePreconfiguredElement)
                .adaptTo('foundation-field')
                .getValue() === 'true';

        displayRecsOptions(usePreconfigured, $dialogContent);

        $(usePreconfiguredElement).on('change', function(e) {
            var changeValue =
                $(e.target)
                    .adaptTo('foundation-field')
                    .getValue() === 'true';

            displayRecsOptions(changeValue, $dialogContent);
        });
    }
})(jQuery);
