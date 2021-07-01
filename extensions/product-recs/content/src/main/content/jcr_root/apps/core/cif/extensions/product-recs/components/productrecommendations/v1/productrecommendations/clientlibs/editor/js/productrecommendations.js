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
    var enableFilterSelector = '.cif-product-recs__enable-filter';

    $(document).on('dialog-loaded', function(e) {
        var $dialog = e.dialog;
        var $dialogContent = $dialog.find(dialogContentSelector);
        var dialogContent = $dialogContent.length > 0 ? $dialogContent[0] : undefined;

        if (dialogContent) {
            init($dialogContent);
        }
    });

    function enableFilter(target, enable, $dialogContent) {
        if (target.data('targetId')) {
            var targetedBy = $dialogContent.find('[target="[data-target-id=\'' + target.data('targetId') + '\']"]');
            targetedBy.adaptTo('foundation-field').setDisabled(!enable);
        } else {
            target.adaptTo('foundation-field').setDisabled(!enable);
        }
    }

    function init($dialogContent) {
        var filters = $dialogContent.find(enableFilterSelector);

        filters.each(function(index, element) {
            var enabled =
                $(element)
                    .adaptTo('foundation-field')
                    .getValue() === 'true';

            var targets = element.dataset.target;

            targets.split(',').forEach(function(target) {
                var targetEl = $dialogContent.find('[name="' + target + '"]');
                enableFilter(targetEl, enabled, $dialogContent);
            });

            $(element).on('change', function(e) {
                var changeValue =
                    $(e.target)
                        .adaptTo('foundation-field')
                        .getValue() === 'true';
                targets.split(',').forEach(function(target) {
                    var targetEl = $dialogContent.find('[name="' + target + '"]');
                    enableFilter(targetEl, changeValue, $dialogContent);
                });
            });
        });
    }
})(jQuery);
