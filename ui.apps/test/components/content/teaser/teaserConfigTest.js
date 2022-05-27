/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
'use strict';

import TeaserConfigV1 from '../../../../src/main/content/jcr_root/apps/core/cif/components/content/teaser/v1/teaser/clientlib/editor/js/teaser';
import TeaserConfigV2 from '../../../../src/main/content/jcr_root/apps/core/cif/components/content/teaser/v2/teaser/clientlib/editor/js/teaser';
import TeaserConfigV3 from '../../../../src/main/content/jcr_root/apps/core/cif/components/content/teaser/v3/teaser/clientlib/editor/js/teaser';
import jQuery from '../../../clientlibs/common/jQueryMockForTest';

[
    ['TeaserConfig v1', TeaserConfigV1],
    ['TeaserConfig v2', TeaserConfigV2],
    ['TeaserConfig v3', TeaserConfigV3]
].forEach(([name, TeaserConfig]) =>
    describe(name, () => {
        var body;
        var dialogRoot;
        var emptyElement;

        before(() => {
            body = window.document.querySelector('body');
            dialogRoot = document.createElement('div');
            body.insertAdjacentElement('afterbegin', dialogRoot);
            emptyElement = document.createElement('div');
            body.insertAdjacentElement('beforeend', emptyElement);

            dialogRoot.insertAdjacentHTML(
                'afterbegin',
                `<div class="cq-dialog-content cmp-teaser__editor" data-cmp-is="commerceteaser-editor">
    <coral-tabview class="coral3-TabView" maximized="">
        <coral-panelstack class="coral3-PanelStack" role="presentation">
            <coral-panel class="coral3-Panel is-selected" role="tabpanel" selected="">
                <coral-panel-content>
                    <div class="foundation-layout-util-vmargin">
                        <div class="coral-FixedColumn foundation-layout-util-vmargin">
                            <div class="coral-FixedColumn-column">
                                <div class="coral-Form-fieldwrapper foundation-toggleable cmp-teaser__editor-link-url">
                                    <label class="coral-Form-fieldlabel">Link</label>
                                    <foundation-autocomplete class="coral-Form-field" data-foundation-validation=""
                                                             disabled=""
                                                             name="./linkURL"
                                                             pickersrc="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/picker.html?root=%2fcontent&amp;selectionCount=single"
                                                             role="combobox">
                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                            <div class="coral-InputGroup">
                                                <input autocomplete="off"
                                                       class="coral3-Textfield coral-InputGroup-input"
                                                       disabled=""
                                                       is="coral-textfield" placeholder="">
                                                <span class="coral-InputGroup-button">
                                                    <button class="coral3-Button coral3-Button--secondary" disabled="" is="coral-button"
                                                            size="M" title="Open Selection Dialog" type="button" variant="secondary">
                                                        <coral-icon class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select"
                                                                    icon="select" role="img" size="S"></coral-icon>
                                                        <coral-button-label></coral-button-label>
                                                    </button>
                                                </span>
                                            </div>
                                        </div>
                                        <coral-overlay class="foundation-picker-buttonlist coral3-Overlay"
                                                       data-foundation-picker-buttonlist-src="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/suggestion{.offset,limit}.html?root=%2fcontent{&amp;query}"
                                                       foundation-autocomplete-suggestion=""
                                                       style="display: none;"></coral-overlay>
                                        <coral-taglist class="coral3-TagList is-disabled" disabled=""
                                                       foundation-autocomplete-value="" name="./linkURL" role="listbox">
                                            <coral-tag class="coral3-Tag coral3-Tag--large" disabled="" role="option" value="">
                                                <button class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton"
                                                        coral-close="" disabled=""
                                                        handle="button" icon="close" iconsize="XS" is="coral-button" size="M"
                                                        tabindex="-1" title="Remove" tracking="off" type="button" variant="minimal">
                                                    <coral-icon class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                                icon="close" role="img" size="XS"></coral-icon>
                                                    <coral-button-label></coral-button-label>
                                                </button>
                                                <coral-tag-label></coral-tag-label>
                                                <input disabled="" handle="input" name="./linkURL" type="hidden" value="">
                                            </coral-tag>
                                            <object data="about:blank"
                                                    style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                    tabindex="-1" type="text/html">​
                                            </object>
                                        </coral-taglist>
                                        <input class="foundation-field-related" disabled="" name="./linkURL@Delete" type="hidden">
                                    </foundation-autocomplete>
                                    <coral-icon alt="description"
                                                class="coral-Form-fieldinfo coral3-Icon coral3-Icon--infoCircle coral3-Icon--sizeS"
                                                icon="infoCircle" role="img" size="S" tabindex="0"></coral-icon>
                                    <coral-tooltip class="coral3-Tooltip coral3-Tooltip--info" placement="left" role="tooltip"
                                                   style="display: none;" tabindex="-1" target="_prev" variant="info">
                                        <coral-tooltip-content>Link applied to teaser elements. URL or path to a content page.
                                        </coral-tooltip-content>
                                    </coral-tooltip>
                                </div>
                                <div class="coral-Form-fieldwrapper coral-Form-fieldwrapper--singleline">
                                    <coral-checkbox checked="" class="coral-Form-field coral3-Checkbox" data-foundation-validation=""
                                                    data-validation="" name="./actionsEnabled" value="true">
                                        <input class=" coral3-Checkbox-input" handle="input" name="./actionsEnabled" type="checkbox"
                                               value="true">
                                        <span class=" coral3-Checkbox-checkmark" handle="checkbox"></span>
                                        <label class=" coral3-Checkbox-description" handle="labelWrapper">
                                            <coral-checkbox-label>Enable Call-To-Action</coral-checkbox-label>
                                        </label>
                                    </coral-checkbox>
                                    <input class="foundation-field-related" name="./actionsEnabled@Delete" type="hidden">
                                    <input class="foundation-field-related" name="./actionsEnabled@DefaultValue" type="hidden"
                                           value="false">
                                    <input class="foundation-field-related" name="./actionsEnabled@UseDefaultWhenMissing" type="hidden"
                                           value="true">
                                    <coral-icon alt="description"
                                                class="coral-Form-fieldinfo coral3-Icon coral3-Icon--infoCircle coral3-Icon--sizeS"
                                                icon="infoCircle" role="img" size="S" tabindex="0"></coral-icon>
                                    <coral-tooltip class="coral3-Tooltip coral3-Tooltip--info coral3-Tooltip--arrowRight" placement="right"
                                                   role="tooltip"
                                                   style="z-index: 10020; max-width: 915.422px; left: 184.578px; top: 2.5px; display: none;"
                                                   tabindex="-1" target="_prev"
                                                   variant="info">
                                        <coral-tooltip-content>When checked, enables definition of Call-To-Actions. The linked page
                                            of the first Call-To-Action is used when populating title and description.
                                        </coral-tooltip-content>
                                    </coral-tooltip>
                                </div>
                                <coral-multifield
                                        class="coral-Form-field foundation-toggleable cmp-teaser__editor-multifield_actions coral3-Multifield"
                                        data-foundation-validation="" data-granite-coral-multifield-composite=""
                                        data-granite-coral-multifield-name="./actions"
                                        data-validation="" role="list">
                                    <coral-multifield-item class="coral3-Multifield-item" role="listitem">
                                        <coral-multifield-item-content>
                                            <div class="cmp-teaser__editor-action">
                                                <div class="coral-Form-fieldwrapper">
                                                    <label class="coral-Form-fieldlabel">Page</label>
                                                    <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                                             data-cmp-teaser-v1-dialog-edit-hook="actionLink"
                                                                             name="./actions/item0/link" placeholder="Path"
                                                                             pickersrc="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/picker.html?_charset_=utf-8&amp;path={value}&amp;root=%2fcontent&amp;selectionCount=single"
                                                                             labelledby="label_71c89c58-e64e-4914-a356-d9ddec189622"
                                                                             data-foundation-validation="">
                                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                                            <div class="coral-InputGroup" role="presentation">
                                                                <input is="coral-textfield"
                                                                         class="coral3-Textfield coral-InputGroup-input"
                                                                         autocomplete="off"
                                                                         placeholder="Path"
                                                                         id="coral-id-618"
                                                                         aria-labelledby="label_71c89c58-e64e-4914-a356-d9ddec189622"
                                                                         role="combobox"
                                                                         aria-label="Path"
                                                                         labelledby="label_71c89c58-e64e-4914-a356-d9ddec189622"
                                                                         aria-invalid="false">
                                                                <span class="coral-InputGroup-button">
                                                                    <button is="coral-button"
                                                                            class="coral3-Button coral3-Button--secondary"
                                                                            size="M" variant="secondary"
                                                                            title="Open Selection Dialog"
                                                                            type="button"
                                                                            aria-label="Open Selection Dialog">
                                                                        <coral-icon
                                                                            class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select" icon="select"
                                                                            size="S" autoarialabel="on" role="img" aria-label="select"></coral-icon>
                                                                        <coral-button-label></coral-button-label>
                                                                    </button>
                                                                </span>
                                                            </div>
                                                        </div>
                                                        <coral-overlay foundation-autocomplete-suggestion=""
                                                                       class="foundation-picker-buttonlist coral3-Overlay"
                                                                       data-foundation-picker-buttonlist-src="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/suggestion{.offset,limit}.html?root=%2fcontent{&amp;query}"
                                                                       aria-hidden="true" style="display: none;"></coral-overlay>
                                                        <coral-taglist foundation-autocomplete-value="" name="./actions/item0/link"
                                                                       class="coral3-TagList" aria-disabled="false" role="list"
                                                                       aria-live="off" aria-atomic="false" aria-relevant="additions"
                                                                       aria-invalid="false">
                                                            <coral-tag value="/content/venia/us/en/sample" tabindex="0"
                                                                       class="coral3-Tag coral3-Tag--large" trackingelement=""
                                                                       role="listitem">
                                                                <button is="coral-button"
                                                                        class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton"
                                                                        size="M" variant="minimal" tracking="off" handle="button"
                                                                        type="button" icon="close" iconsize="XS" title="Remove"
                                                                        tabindex="-1" coral-close="">
                                                                    <coral-icon class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                                                icon="close" size="XS" autoarialabel="on" role="img"
                                                                                aria-label="close"></coral-icon>
                                                                    <coral-button-label></coral-button-label>
                                                                </button>
                                                                <coral-tag-label>/content/venia/us/en/sample</coral-tag-label>
                                                                <input handle="input" type="hidden" name="./actions/item0/link"
                                                                       value="/content/venia/us/en/sample"></coral-tag>
                                                            <object aria-hidden="true" tabindex="-1"
                                                                    style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                                    type="text/html" data="about:blank">​
                                                            </object>
                                                        </coral-taglist>
                                                        <input class="foundation-field-related" type="hidden"
                                                               name="./actions/item0/link@Delete"></foundation-autocomplete>
                                                </div>
                                                <div class="coral-Form-fieldwrapper coral-Form-fieldwrapper--singleline">
                                                    <coral-checkbox class="cmp-teaser__editor-actionField-linkTarget coral-Form-field _coral-Checkbox" data-cmp-teaser-v2-dialog-edit-hook="actionTarget" name="./actions/item0/./linkTarget" value="_blank" labelledby="description_866fc36e-46a1-4923-9f15-15a46e861631" data-foundation-validation="" data-validation="">
                                                        <input type="checkbox" handle="input" class=" _coral-Checkbox-input" id="coral-id-577" aria-labelledby="description_866fc36e-46a1-4923-9f15-15a46e861631" name="./actions/item0/./linkTarget" value="_blank">
                                                        <span class=" _coral-Checkbox-box" handle="checkbox"></span>
                                                        <label class=" _coral-Checkbox-label" handle="labelWrapper" for="coral-id-577" style="margin: 0px;">
                                                            <span class=" u-coral-screenReaderOnly" handle="screenReaderOnly" hidden="">Select</span>
                                                            <coral-checkbox-label></coral-checkbox-label>
                                                        </label>
                                                    </coral-checkbox>
                                                    <input class="foundation-field-related" type="hidden" name="./actions/item0/./linkTarget@Delete">
                                                    <input class="foundation-field-related" type="hidden" value="_self" name="./actions/item0/./linkTarget@DefaultValue">
                                                    <input class="foundation-field-related" type="hidden" value="true" name="./actions/item0/./linkTarget@UseDefaultWhenMissing">
                                                    <coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS" icon="infoCircle" tabindex="0" aria-describedby="description_866fc36e-46a1-4923-9f15-15a46e861631" alt="description" role="img" aria-label="description" size="S"></coral-icon>
                                                    <coral-tooltip target="_prev" placement="right" id="description_866fc36e-46a1-4923-9f15-15a46e861631" x-placement="left" x-out-of-boundaries="" class="_coral-Overlay _coral-Tooltip _coral-Tooltip--default _coral-Tooltip--left" role="tooltip" tabindex="-1" variant="default">                                                                
                                                        <span class=" _coral-Tooltip-tip" handle="tip"></span>
                                                        <coral-tooltip-content class="_coral-Tooltip-label">If checked the link will be opened in a new browser tab.</coral-tooltip-content>
                                                    </coral-tooltip>
                                                </div>                                            
                                                <div class="coral-Form-fieldwrapper">
                                                    <label class="coral-Form-fieldlabel">Product</label>
                                                    <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                                             data-cmp-teaser-v1-dialog-edit-hook="actionProduct"
                                                                             data-foundation-validation=""
                                                                             name="./actions/item0/productSlug"
                                                                             pickersrc="/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;selectionCount=single&amp;selectionId=slug"
                                                                             placeholder="Product slug" role="combobox">
                                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                                            <div class="coral-InputGroup">
                                                                <input autocomplete="off"
                                                                       class="coral3-Textfield coral-InputGroup-input"
                                                                       is="coral-textfield"
                                                                       placeholder="Product slug">
                                                                <span class="coral-InputGroup-button">
                                                                    <button class="coral3-Button coral3-Button--secondary"
                                                                            is="coral-button"
                                                                            size="M"
                                                                            title="Open Selection Dialog"
                                                                            type="button"
                                                                            variant="secondary">
                                                                        <coral-icon
                                                                                class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select"
                                                                                icon="select" role="img" size="S">
                                                                        </coral-icon>
                                                                        <coral-button-label></coral-button-label>
                                                                    </button>
                                                                </span>
                                                            </div>
                                                        </div>
                                                        <coral-overlay class="foundation-picker-buttonlist coral3-Overlay"
                                                                       data-foundation-picker-buttonlist-src=""
                                                                       foundation-autocomplete-suggestion="" style="display: none;">
                                                        </coral-overlay>
                                                        <coral-taglist class="coral3-TagList"
                                                                       foundation-autocomplete-value="" name="./actions/item0/productSlug"
                                                                       role="listbox">
                                                            <object data="about:blank"
                                                                    style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                                    tabindex="-1" type="text/html">​
                                                            </object>
                                                            <coral-tag class="coral3-Tag coral3-Tag--large" closable=""
                                                                       role="option" tabindex="0">
                                                                <button class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton"
                                                                        coral-close=""
                                                                        handle="button" icon="close" iconsize="XS" is="coral-button"
                                                                        size="M" tabindex="-1" title="Remove" tracking="off"
                                                                        type="button" variant="minimal">
                                                                    <coral-icon
                                                                            class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                                            icon="close" role="img" size="XS">
                                                                    </coral-icon>
                                                                    <coral-button-label></coral-button-label>
                                                                </button>
                                                                <coral-tag-label></coral-tag-label>
                                                                <input handle="input"
                                                                       name="./actions/item0/productSlug" type="hidden" value="">
                                                            </coral-tag>
                                                        </coral-taglist>
                                                        <input class="foundation-field-related" name="./actions/item0/productSlug@Delete"
                                                               type="hidden">
                                                    </foundation-autocomplete>
                                                </div>
                                                <div class="coral-Form-fieldwrapper">
                                                    <label class="coral-Form-fieldlabel">Category</label>
                                                    <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                                             data-cmp-teaser-v1-dialog-edit-hook="actionCategory"
                                                                             data-foundation-validation="" name="./actions/item0/categoryId"
                                                                             pickersrc="/mnt/overlay/commerce/gui/content/common/cifcategoryfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;filter=folderOrCategory&amp;selectionCount=single&amp;selectionId=id"
                                                                             placeholder="Category ID" role="combobox">
                                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                                            <div class="coral-InputGroup">
                                                                <input autocomplete="off"
                                                                       class="coral3-Textfield coral-InputGroup-input"
                                                                       is="coral-textfield"
                                                                       placeholder="Category ID">
                                                                <span class="coral-InputGroup-button">
                                                                    <button
                                                                            class="coral3-Button coral3-Button--secondary"
                                                                            is="coral-button"
                                                                            size="M"
                                                                            title="Open Selection Dialog"
                                                                            type="button"
                                                                            variant="secondary">
                                                                        <coral-icon
                                                                                class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select"
                                                                                icon="select" role="img" size="S">
                                                                        </coral-icon>
                                                                        <coral-button-label></coral-button-label>
                                                                    </button>
                                                                </span>
                                                            </div>
                                                        </div>
                                                        <coral-overlay class="foundation-picker-buttonlist coral3-Overlay"
                                                                       data-foundation-picker-buttonlist-src=""
                                                                       foundation-autocomplete-suggestion="" style="display: none;">
                                                        </coral-overlay>
                                                        <coral-taglist class="coral3-TagList"
                                                                       foundation-autocomplete-value="" name="./actions/item0/categoryId"
                                                                       role="listbox">
                                                            <object data="about:blank"
                                                                    style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                                    tabindex="-1" type="text/html">​
                                                            </object>
                                                            <coral-tag class="coral3-Tag coral3-Tag--large" closable=""
                                                                       role="option" tabindex="0">
                                                                <button class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton"
                                                                        coral-close=""
                                                                        handle="button" icon="close" iconsize="XS" is="coral-button"
                                                                        size="M" tabindex="-1" title="Remove" tracking="off"
                                                                        type="button" variant="minimal">
                                                                    <coral-icon
                                                                            class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                                            icon="close" role="img" size="XS">
                                                                    </coral-icon>
                                                                    <coral-button-label></coral-button-label>
                                                                </button>
                                                                <coral-tag-label></coral-tag-label>
                                                                <input handle="input"
                                                                       name="./actions/item0/categoryId" type="hidden" value="">
                                                            </coral-tag>
                                                        </coral-taglist>
                                                        <input class="foundation-field-related" name="./actions/item0/categoryId@Delete"
                                                               type="hidden">
                                                    </foundation-autocomplete>
                                                </div>
                                                <div class="coral-Form-fieldwrapper">
                                                    <label class="coral-Form-fieldlabel">Link Text *</label>
                                                    <input
                                                        class="coral-Form-field cmp-teaser__editor-actionField coral3-Textfield"
                                                        data-cmp-teaser-v1-dialog-edit-hook="actionTitle" data-foundation-validation=""                                                        
                                                        data-validation="" is="coral-textfield" 
                                                        data-cmp-teaser-v2-dialog-edit-hook="actionTitle" name="./actions/item0/text"
                                                        placeholder="Text" type="text" value="">
                                                </div>
                                            </div>
                                        </coral-multifield-item-content>
                                        <button class="coral3-Button coral3-Button--quiet coral3-Multifield-remove"
                                                handle="remove" icon="delete"
                                                iconsize="S" is="coral-button" size="M" tracking="off" type="button" variant="quiet">
                                            <coral-icon class="coral3-Icon coral3-Icon--sizeS coral3-Icon--delete" icon="delete"
                                                        role="img" size="S"></coral-icon>
                                            <coral-button-label></coral-button-label>
                                        </button>
                                        <button class="coral3-Button coral3-Button--quiet coral3-Multifield-move u-coral-openHand"
                                                handle="move"
                                                icon="moveUpDown" iconsize="S" is="coral-button" size="M" tracking="off" type="button"
                                                variant="quiet">
                                            <coral-icon class="coral3-Icon coral3-Icon--sizeS coral3-Icon--moveUpDown"
                                                        icon="moveUpDown" role="img" size="S"></coral-icon>
                                            <coral-button-label></coral-button-label>
                                        </button>
                                    </coral-multifield-item>
                                    <button class="coral3-Button coral3-Button--secondary" coral-multifield-add="" is="coral-button"
                                            size="M" type="button" variant="secondary">
                                        <coral-button-label>Add</coral-button-label>
                                    </button>
                                    <input class="foundation-field-related" name="./actions@Delete" type="hidden">
                                    <template coral-multifield-template="">
                                        <div class="cmp-teaser__editor-action">
                                            <div class="coral-Form-fieldwrapper">
                                                <label class="coral-Form-fieldlabel">Page</label>
                                                <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                                         data-cmp-teaser-v1-dialog-edit-hook="actionLink" name="link"
                                                                         placeholder="Path"
                                                                         pickersrc="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/picker.html?_charset_=utf-8&amp;path={value}&amp;root=%2fcontent&amp;selectionCount=single"
                                                                         labelledby="label_73842f7d-ae0c-4a00-b841-d824a6e3adc5"
                                                                         data-foundation-validation="">
                                                    <coral-overlay foundation-autocomplete-suggestion=""
                                                                   class="foundation-picker-buttonlist"
                                                                   data-foundation-picker-buttonlist-src="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/suggestion{.offset,limit}.html?root=%2fcontent{&amp;query}"></coral-overlay>
                                                    <coral-taglist foundation-autocomplete-value="" name="link">
                                                        <coral-tag value=""></coral-tag>
                                                    </coral-taglist>
                                                    <input class="foundation-field-related" type="hidden" name="link@Delete">
                                                </foundation-autocomplete>
                                            </div>
                                            <div class="coral-Form-fieldwrapper">
                                                <label class="coral-Form-fieldlabel">Product</label>
                                                <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                                         data-cmp-teaser-v1-dialog-edit-hook="actionProduct"
                                                                         data-foundation-validation=""
                                                                         name="productSlug"
                                                                         pickersrc="/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;selectionCount=single&amp;selectionId=slug"
                                                                         placeholder="Product slug">
                                                    <coral-overlay class="foundation-picker-buttonlist"
                                                                   data-foundation-picker-buttonlist-src=""
                                                                   foundation-autocomplete-suggestion=""></coral-overlay>
                                                    <coral-taglist foundation-autocomplete-value="" name="productSlug">
                                                        <coral-tag value=""></coral-tag>
                                                    </coral-taglist>
                                                    <input class="foundation-field-related" name="productSlug@Delete" type="hidden">
                                                </foundation-autocomplete>
                                            </div>
                                            <div class="coral-Form-fieldwrapper">
                                                <label class="coral-Form-fieldlabel">Category</label>
                                                <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                                         data-cmp-teaser-v1-dialog-edit-hook="actionCategory"
                                                                         data-foundation-validation=""
                                                                         name="categoryId"
                                                                         pickersrc="/mnt/overlay/commerce/gui/content/common/cifcategoryfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;filter=folderOrCategory&amp;selectionCount=single&amp;selectionId=id"
                                                                         placeholder="Category ID">
                                                    <coral-overlay class="foundation-picker-buttonlist"
                                                                   data-foundation-picker-buttonlist-src=""
                                                                   foundation-autocomplete-suggestion=""></coral-overlay>
                                                    <coral-taglist foundation-autocomplete-value="" name="categoryId">
                                                        <coral-tag value=""></coral-tag>
                                                    </coral-taglist>
                                                    <input class="foundation-field-related" name="categoryId@Delete" type="hidden">
                                                </foundation-autocomplete>
                                            </div>
                                            <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Link Text*</label>
                                                <input class="coral-Form-field cmp-teaser__editor-actionField"
                                                       data-cmp-teaser-v1-dialog-edit-hook="actionTitle"
                                                       data-foundation-validation="" data-validation=""
                                                       is="coral-textfield" name="text" placeholder="Text" type="text"
                                                       value="">
                                            </div>
                                        </div>
                                    </template>
                                </coral-multifield>
                            </div>
                        </div>
                    </div>
                </coral-panel-content>
            </coral-panel>
        </coral-panelstack>
    </coral-tabview>
</div>`
            );
        });

        after(() => {
            body.removeChild(dialogRoot);
        });

        it('initializes the TeaserConfig component', () => {
            const spyOn = sinon.spy();
            const fakeJQuery = sinon.stub().returns({
                on: spyOn
            });

            const teaserConfig = new TeaserConfig(fakeJQuery);
            assert(
                spyOn.calledOnceWith('dialog-loaded', teaserConfig.handleDialogLoaded),
                'Event subscription not happening'
            );
        });

        it('handles dialog open event', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const stubAttachEventHandlers = sinon.stub(teaserConfig, 'attachEventHandlers');
            const stubActionsToggleHandler = sinon.stub(teaserConfig, 'actionsToggleHandler');
            const wrongEvent = {
                dialog: jQuery(emptyElement)
            };

            teaserConfig.handleDialogLoaded(wrongEvent);

            assert(stubAttachEventHandlers.notCalled);
            assert(stubActionsToggleHandler.notCalled);

            const event = {
                dialog: jQuery(dialogRoot)
            };

            teaserConfig.handleDialogLoaded(event);

            assert(stubAttachEventHandlers.calledOnce, 'Dialog not found');
            assert(stubActionsToggleHandler.calledOnce, 'Dialog not found');

            teaserConfig.attachEventHandlers.restore();
            teaserConfig.actionsToggleHandler.restore();
        });

        it('attaches field change handlers', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const stubHandlePickersChange = sinon.stub(teaserConfig, 'handlePickersChange');
            const multiFieldActions = {};
            const stubJQuery = sinon.stub(teaserConfig, '$');
            const jMultiFieldActions = {
                on: sinon.fake()
            };
            stubJQuery.withArgs(multiFieldActions).returns(jMultiFieldActions);

            teaserConfig.attachEventHandlers(multiFieldActions);

            assert(stubHandlePickersChange.calledOnceWith(multiFieldActions));
            assert(jMultiFieldActions.on.calledOnceWith('change'));

            teaserConfig.handlePickersChange.restore();
        });

        it('handles toggling actions', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const actionsEnabledCheckbox = {
                addEventListener: sinon.fake()
            };
            teaserConfig.actionsToggleHandler(actionsEnabledCheckbox);
            assert(actionsEnabledCheckbox.addEventListener.calledOnceWith('change'));
        });

        it('handles toggling fields', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const actionsEnabledCheckbox = document.querySelector(
                TeaserConfig.selectors.actionsEnabledCheckboxSelector
            );
            teaserConfig.actionsToggleHandler(actionsEnabledCheckbox);

            actionsEnabledCheckbox.checked = false;
            actionsEnabledCheckbox.dispatchEvent(new Event('change'));

            const prodDisabled = document.querySelector(TeaserConfig.selectors.productFieldSelector).disabled;
            const catDisabled = document.querySelector(TeaserConfig.selectors.categoryFieldSelector).disabled;

            actionsEnabledCheckbox.checked = true;
            actionsEnabledCheckbox.dispatchEvent(new Event('change'));

            assert.equal(document.querySelector(TeaserConfig.selectors.productFieldSelector).disabled, !prodDisabled);
            assert.equal(document.querySelector(TeaserConfig.selectors.categoryFieldSelector).disabled, !catDisabled);
        });

        it('handles pickers change', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const stubJQuery = sinon.stub(teaserConfig, '$');

            const pageElement = {
                off: sinon.fake(),
                on: sinon.fake(),
                adaptTo: sinon
                    .stub()
                    .withArgs('foundation-field')
                    .returns('page-field')
            };

            const productElement = {
                off: sinon.fake(),
                on: sinon.fake(),
                adaptTo: sinon
                    .stub()
                    .withArgs('foundation-field')
                    .returns('product-field')
            };

            const categoryElement = {
                off: sinon.fake(),
                on: sinon.fake(),
                adaptTo: sinon
                    .stub()
                    .withArgs('foundation-field')
                    .returns('category-field')
            };

            const eventData = {
                pageField: 'page-field',
                productField: 'product-field',
                categoryField: 'category-field'
            };

            stubJQuery.withArgs(document.querySelector(TeaserConfig.selectors.pageFieldSelector)).returns(pageElement);
            stubJQuery
                .withArgs(document.querySelector(TeaserConfig.selectors.productFieldSelector))
                .returns(productElement);
            stubJQuery
                .withArgs(document.querySelector(TeaserConfig.selectors.categoryFieldSelector))
                .returns(categoryElement);

            const multiFieldActions = document.querySelector(TeaserConfig.selectors.actionsMultifieldSelector);
            teaserConfig.handlePickersChange(multiFieldActions);

            assert(pageElement.off.calledOnceWithExactly('change', teaserConfig.handlePageChange));
            assert(productElement.off.calledOnceWithExactly('change', teaserConfig.handleProductChange));
            assert(categoryElement.off.calledOnceWithExactly('change', teaserConfig.handleCategoryChange));

            assert(pageElement.adaptTo.calledOnceWithExactly('foundation-field'));
            assert(productElement.adaptTo.calledOnceWithExactly('foundation-field'));
            assert(categoryElement.adaptTo.calledOnceWithExactly('foundation-field'));

            assert(pageElement.on.calledOnceWithExactly('change', eventData, teaserConfig.handlePageChange));
            assert(productElement.on.calledOnceWithExactly('change', eventData, teaserConfig.handleProductChange));
            assert(categoryElement.on.calledOnceWithExactly('change', eventData, teaserConfig.handleCategoryChange));
        });

        it('handles page field change', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const pageField = {
                getValue: sinon.fake.returns('')
            };
            const productField = {
                setValue: sinon.fake()
            };
            const categoryField = {
                setValue: sinon.fake()
            };

            teaserConfig.handlePageChange({ data: { pageField, productField, categoryField } });
            assert(productField.setValue.notCalled);
            assert(categoryField.setValue.notCalled);

            pageField.getValue = sinon.fake.returns('test');
            teaserConfig.handlePageChange({ data: { pageField, productField, categoryField } });
            assert(productField.setValue.calledOnceWithExactly(''));
            assert(categoryField.setValue.calledOnceWithExactly(''));
        });

        it('handles product field change', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const pageField = {
                setValue: sinon.fake()
            };
            const productField = {
                getValue: sinon.fake.returns('')
            };
            const categoryField = {
                setValue: sinon.fake()
            };

            teaserConfig.handleProductChange({ data: { pageField, productField, categoryField } });
            assert(pageField.setValue.notCalled);
            assert(categoryField.setValue.notCalled);

            productField.getValue = sinon.fake.returns('test');
            teaserConfig.handleProductChange({ data: { pageField, productField, categoryField } });
            assert(pageField.setValue.calledOnceWithExactly(''));
            assert(categoryField.setValue.calledOnceWithExactly(''));
        });

        it('handles category field change', () => {
            const teaserConfig = new TeaserConfig(jQuery);
            const pageField = {
                setValue: sinon.fake()
            };
            const categoryField = {
                getValue: sinon.fake.returns('')
            };
            const productField = {
                setValue: sinon.fake()
            };

            teaserConfig.handleCategoryChange({ data: { pageField, productField, categoryField } });
            assert(pageField.setValue.notCalled);
            assert(productField.setValue.notCalled);

            categoryField.getValue = sinon.fake.returns('test');
            teaserConfig.handleCategoryChange({ data: { pageField, productField, categoryField } });
            assert(pageField.setValue.calledOnceWithExactly(''));
            assert(productField.setValue.calledOnceWithExactly(''));
        });
    })
);
