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
(function($) {
    'use strict';

    var dialogContentSelector = '.cif-product-carousel__editor';
    var currentSelectionTypeSelector = 'input[name="./selectionType"]:checked';
    var selectionTypeSelector = 'coral-radio[name="./selectionType"]';
    var productsConfigSelector = '.cif-product-carousel__products-config';
    var categoryConfigSelector = '.cif-product-carousel__category-config';

    $(document).on('dialog-loaded', function(e) {
        var $dialog = e.dialog;
        var $dialogContent = $dialog.find(dialogContentSelector);
        var dialogContent = $dialogContent.length > 0 ? $dialogContent[0] : undefined;

        if (dialogContent) {
            init($dialogContent);
        }
    });

    function applySelectionTypeChange(value) {
        if (value === 'product') {
            $(categoryConfigSelector).hide();
            $(productsConfigSelector).show();
        } else if (value === 'category') {
            $(productsConfigSelector).hide();
            $(categoryConfigSelector).show();
        }
    }

    function init($dialogContent) {
        var currentSelectionType = $dialogContent[0].querySelector(currentSelectionTypeSelector);
        if (currentSelectionType) {
            applySelectionTypeChange(currentSelectionType.value);

            $dialogContent.on('change', selectionTypeSelector, function(e) {
                applySelectionTypeChange(e.target.value);
            });
        }
    }
})(jQuery);
