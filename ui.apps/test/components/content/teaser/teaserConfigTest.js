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

import TeaserConfig from '../../../../src/main/content/jcr_root/apps/core/cif/components/content/teaser/v1/teaser/clientlib/editor/js/teaser';
import jQuery from '../../../clientlibs/common/jQueryMockForTest';

describe('TeaserConfig', () => {
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
            <coral-tabview maximized="" class="coral3-TabView">
            <coral-panelstack class="coral3-PanelStack" role="presentation">
            <coral-panel class="coral3-Panel is-selected" role="tabpanel" selected="">
                <coral-panel-content>
                    <div class="foundation-layout-util-vmargin">
                        <div class="coral-FixedColumn foundation-layout-util-vmargin">
                            <div class="coral-FixedColumn-column">
                                <div class="coral-Form-fieldwrapper foundation-toggleable cmp-teaser__editor-link-url"><label
                                        class="coral-Form-fieldlabel">Link</label>
                                    <foundation-autocomplete class="coral-Form-field" name="./linkURL"
                                        pickersrc="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/picker.html?root=%2fcontent&amp;selectionCount=single"
                                        data-foundation-validation="" role="combobox" disabled="">
                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                            <div class="coral-InputGroup"><input is="coral-textfield"
                                                    class="coral3-Textfield coral-InputGroup-input" autocomplete="off"
                                                    placeholder="" disabled=""><span class="coral-InputGroup-button"><button
                                                        is="coral-button" class="coral3-Button coral3-Button--secondary" size="M"
                                                        variant="secondary" title="Open Selection Dialog" type="button" disabled="">
                                                        <coral-icon class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select"
                                                            icon="select" size="S" role="img"></coral-icon>
                                                        <coral-button-label></coral-button-label>
                                                    </button></span></div>
                                        </div>
                                        <coral-overlay foundation-autocomplete-suggestion=""
                                            class="foundation-picker-buttonlist coral3-Overlay"
                                            data-foundation-picker-buttonlist-src="/mnt/overlay/cq/gui/content/coral/common/form/pagefield/suggestion{.offset,limit}.html?root=%2fcontent{&amp;query}"
                                            style="display: none;"></coral-overlay>
                                        <coral-taglist foundation-autocomplete-value="" name="./linkURL"
                                            class="coral3-TagList is-disabled" role="listbox" disabled="">
                                            <coral-tag value="" class="coral3-Tag coral3-Tag--large" role="option" disabled="">
                                                <button is="coral-button"
                                                    class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton" size="M"
                                                    variant="minimal" tracking="off" handle="button" type="button" icon="close"
                                                    iconsize="XS" title="Remove" tabindex="-1" coral-close="" disabled="">
                                                    <coral-icon class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                        icon="close" size="XS" role="img"></coral-icon>
                                                    <coral-button-label></coral-button-label>
                                                </button>
                                                <coral-tag-label></coral-tag-label><input handle="input" type="hidden"
                                                    name="./linkURL" value="" disabled="">
                                            </coral-tag><object tabindex="-1"
                                                style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                type="text/html" data="about:blank">​</object>
                                        </coral-taglist><input class="foundation-field-related" type="hidden"
                                            name="./linkURL@Delete" disabled="">
                                    </foundation-autocomplete>
                                    <coral-icon class="coral-Form-fieldinfo coral3-Icon coral3-Icon--infoCircle coral3-Icon--sizeS"
                                        icon="infoCircle" tabindex="0" alt="description" size="S" role="img"></coral-icon>
                                    <coral-tooltip target="_prev" placement="left" class="coral3-Tooltip coral3-Tooltip--info"
                                        variant="info" tabindex="-1" role="tooltip" style="display: none;">

                                        <coral-tooltip-content>Link applied to teaser elements. URL or path to a content page.
                                        </coral-tooltip-content>
                                    </coral-tooltip>
                                </div>
                                <div class="coral-Form-fieldwrapper coral-Form-fieldwrapper--singleline">
                                    <coral-checkbox name="./actionsEnabled" value="true" data-foundation-validation=""
                                        data-validation="" class="coral-Form-field coral3-Checkbox" checked=""><input
                                            type="checkbox" handle="input" class=" coral3-Checkbox-input" value="true"
                                            name="./actionsEnabled">
                                        <span class=" coral3-Checkbox-checkmark" handle="checkbox"></span>
                                        <label class=" coral3-Checkbox-description" handle="labelWrapper">
                                            <coral-checkbox-label>Enable Call-To-Action

                                            </coral-checkbox-label>
                                        </label></coral-checkbox><input class="foundation-field-related" type="hidden"
                                        name="./actionsEnabled@Delete"><input class="foundation-field-related" type="hidden"
                                        value="false" name="./actionsEnabled@DefaultValue"><input class="foundation-field-related"
                                        type="hidden" value="true" name="./actionsEnabled@UseDefaultWhenMissing">
                                    <coral-icon class="coral-Form-fieldinfo coral3-Icon coral3-Icon--infoCircle coral3-Icon--sizeS"
                                        icon="infoCircle" tabindex="0" alt="description" size="S" role="img"></coral-icon>
                                    <coral-tooltip target="_prev" placement="right"
                                        class="coral3-Tooltip coral3-Tooltip--info coral3-Tooltip--arrowRight" variant="info"
                                        tabindex="-1" role="tooltip"
                                        style="z-index: 10020; max-width: 915.422px; left: 184.578px; top: 2.5px; display: none;">

                                        <coral-tooltip-content>When checked, enables definition of Call-To-Actions. The linked page
                                            of the first Call-To-Action is used when populating title and description.
                                        </coral-tooltip-content>
                                    </coral-tooltip>
                                </div>
                                <coral-multifield
                                    class="coral-Form-field foundation-toggleable cmp-teaser__editor-multifield_actions coral3-Multifield"
                                    data-foundation-validation="" data-validation="" data-granite-coral-multifield-name="./actions"
                                    data-granite-coral-multifield-composite="" role="list">
                                    <coral-multifield-item class="coral3-Multifield-item" role="listitem">
                                        <coral-multifield-item-content>
                                            <div class="cmp-teaser__editor-action">


                                                <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Product
                                                        Page</label>
                                                    <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                        data-cmp-teaser-v1-dialog-edit-hook="actionLink"
                                                        name="./actions/item0/productSlug" placeholder="Product"
                                                        pickersrc="/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;filter=folderOrProduct&amp;selectionCount=single&amp;selectionId=slug"
                                                        data-foundation-validation="" role="combobox">
                                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                                            <div class="coral-InputGroup"><input is="coral-textfield"
                                                                    class="coral3-Textfield coral-InputGroup-input"
                                                                    autocomplete="off" placeholder="Product"><span
                                                                    class="coral-InputGroup-button"><button is="coral-button"
                                                                        class="coral3-Button coral3-Button--secondary" size="M"
                                                                        variant="secondary" title="Open Selection Dialog"
                                                                        type="button">
                                                                        <coral-icon
                                                                            class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select"
                                                                            icon="select" size="S" role="img">
                                                                        </coral-icon>
                                                                        <coral-button-label></coral-button-label>
                                                                    </button></span></div>
                                                        </div>
                                                        <coral-overlay foundation-autocomplete-suggestion=""
                                                            class="foundation-picker-buttonlist coral3-Overlay"
                                                            data-foundation-picker-buttonlist-src="" style="display: none;">
                                                        </coral-overlay>
                                                        <coral-taglist foundation-autocomplete-value=""
                                                            name="./actions/item0/productSlug" class="coral3-TagList"
                                                            role="listbox"><object tabindex="-1"
                                                                style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                                type="text/html" data="about:blank">​</object>
                                                            <coral-tag class="coral3-Tag coral3-Tag--large" closable=""
                                                                role="option" tabindex="0"><button is="coral-button"
                                                                    class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton"
                                                                    size="M" variant="minimal" tracking="off" handle="button"
                                                                    type="button" icon="close" iconsize="XS" title="Remove"
                                                                    tabindex="-1" coral-close="">
                                                                    <coral-icon
                                                                        class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                                        icon="close" size="XS" role="img">
                                                                    </coral-icon>
                                                                    <coral-button-label></coral-button-label>
                                                                </button>
                                                                <coral-tag-label></coral-tag-label><input handle="input"
                                                                    type="hidden" name="./actions/item0/productSlug" value="">
                                                            </coral-tag>
                                                        </coral-taglist><input class="foundation-field-related" type="hidden"
                                                            name="./actions/item0/productSlug@Delete">
                                                    </foundation-autocomplete>
                                                </div>

                                                <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Category
                                                        Page</label>
                                                    <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                        data-cmp-teaser-v1-dialog-edit-hook="actionLink"
                                                        name="./actions/item0/categoryId" placeholder="Category"
                                                        pickersrc="/mnt/overlay/commerce/gui/content/common/cifcategoryfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;filter=folderOrCategory&amp;selectionCount=single&amp;selectionId=id"
                                                        data-foundation-validation="" role="combobox">
                                                        <div class="foundation-autocomplete-inputgroupwrapper">
                                                            <div class="coral-InputGroup"><input is="coral-textfield"
                                                                    class="coral3-Textfield coral-InputGroup-input"
                                                                    autocomplete="off" placeholder="Category"><span
                                                                    class="coral-InputGroup-button"><button is="coral-button"
                                                                        class="coral3-Button coral3-Button--secondary" size="M"
                                                                        variant="secondary" title="Open Selection Dialog"
                                                                        type="button">
                                                                        <coral-icon
                                                                            class="coral3-Icon coral3-Icon--sizeS coral3-Icon--select"
                                                                            icon="select" size="S" role="img">
                                                                        </coral-icon>
                                                                        <coral-button-label></coral-button-label>
                                                                    </button></span></div>
                                                        </div>
                                                        <coral-overlay foundation-autocomplete-suggestion=""
                                                            class="foundation-picker-buttonlist coral3-Overlay"
                                                            data-foundation-picker-buttonlist-src="" style="display: none;">
                                                        </coral-overlay>
                                                        <coral-taglist foundation-autocomplete-value=""
                                                            name="./actions/item0/categoryId" class="coral3-TagList" role="listbox">
                                                            <object tabindex="-1"
                                                                style="display:block; position:absolute; top:0; left:0; height:100%; width:100%; opacity:0; overflow:hidden; z-index:-100;"
                                                                type="text/html" data="about:blank">​</object>
                                                            <coral-tag class="coral3-Tag coral3-Tag--large" closable=""
                                                                role="option" tabindex="0"><button is="coral-button"
                                                                    class="coral3-Button coral3-Button--minimal coral3-Tag-removeButton"
                                                                    size="M" variant="minimal" tracking="off" handle="button"
                                                                    type="button" icon="close" iconsize="XS" title="Remove"
                                                                    tabindex="-1" coral-close="">
                                                                    <coral-icon
                                                                        class="coral3-Icon coral3-Icon--close coral3-Icon--sizeXS"
                                                                        icon="close" size="XS" role="img">
                                                                    </coral-icon>
                                                                    <coral-button-label></coral-button-label>
                                                                </button>
                                                                <coral-tag-label></coral-tag-label><input handle="input"
                                                                    type="hidden" name="./actions/item0/categoryId" value="">
                                                            </coral-tag>
                                                        </coral-taglist><input class="foundation-field-related" type="hidden"
                                                            name="./actions/item0/categoryId@Delete">
                                                    </foundation-autocomplete>
                                                </div>
                                                <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Link Text
                                                        *</label><input
                                                        class="coral-Form-field cmp-teaser__editor-actionField coral3-Textfield"
                                                        data-cmp-teaser-v1-dialog-edit-hook="actionTitle" type="text"
                                                        name="./actions/item0/text" placeholder="Text" value=""
                                                        data-foundation-validation="" data-validation="" is="coral-textfield"></div>
                                            </div>
                                        </coral-multifield-item-content><button is="coral-button"
                                            class="coral3-Button coral3-Button--quiet coral3-Multifield-remove" size="M"
                                            variant="quiet" type="button" handle="remove" icon="delete" iconsize="S" tracking="off">
                                            <coral-icon class="coral3-Icon coral3-Icon--sizeS coral3-Icon--delete" icon="delete"
                                                size="S" role="img"></coral-icon>
                                            <coral-button-label></coral-button-label>
                                        </button>
                                        <button is="coral-button"
                                            class="coral3-Button coral3-Button--quiet coral3-Multifield-move u-coral-openHand"
                                            size="M" variant="quiet" type="button" handle="move" icon="moveUpDown" iconsize="S"
                                            tracking="off">
                                            <coral-icon class="coral3-Icon coral3-Icon--sizeS coral3-Icon--moveUpDown"
                                                icon="moveUpDown" size="S" role="img"></coral-icon>
                                            <coral-button-label></coral-button-label>
                                        </button>
                                    </coral-multifield-item><button type="button" is="coral-button" coral-multifield-add=""
                                        class="coral3-Button coral3-Button--secondary" size="M" variant="secondary">
                                        <coral-button-label>Add</coral-button-label>
                                    </button>
                                    <input class="foundation-field-related" type="hidden" name="./actions@Delete"><template
                                        coral-multifield-template="">
                                        <div class="cmp-teaser__editor-action">

                                            <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Product
                                                    Page</label>
                                                <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                    data-cmp-teaser-v1-dialog-edit-hook="actionLink" name="productSlug"
                                                    placeholder="Product"
                                                    pickersrc="/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;filter=folderOrProduct&amp;selectionCount=single&amp;selectionId=slug"
                                                    data-foundation-validation="">
                                                    <coral-overlay foundation-autocomplete-suggestion=""
                                                        class="foundation-picker-buttonlist"
                                                        data-foundation-picker-buttonlist-src=""></coral-overlay>
                                                    <coral-taglist foundation-autocomplete-value="" name="productSlug">
                                                        <coral-tag value=""></coral-tag>
                                                    </coral-taglist><input class="foundation-field-related" type="hidden"
                                                        name="productSlug@Delete">
                                                </foundation-autocomplete>
                                            </div>
                                            <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Category
                                                    Page</label>
                                                <foundation-autocomplete class="coral-Form-field cmp-teaser__editor-actionField"
                                                    data-cmp-teaser-v1-dialog-edit-hook="actionLink" name="categoryId"
                                                    placeholder="Category"
                                                    pickersrc="/mnt/overlay/commerce/gui/content/common/cifcategoryfield/picker.html?root=%2fvar%2fcommerce%2fproducts&amp;filter=folderOrCategory&amp;selectionCount=single&amp;selectionId=id"
                                                    data-foundation-validation="">
                                                    <coral-overlay foundation-autocomplete-suggestion=""
                                                        class="foundation-picker-buttonlist"
                                                        data-foundation-picker-buttonlist-src=""></coral-overlay>
                                                    <coral-taglist foundation-autocomplete-value="" name="categoryId">
                                                        <coral-tag value=""></coral-tag>
                                                    </coral-taglist><input class="foundation-field-related" type="hidden"
                                                        name="categoryId@Delete">
                                                </foundation-autocomplete>
                                            </div>
                                            <div class="coral-Form-fieldwrapper"><label class="coral-Form-fieldlabel">Link Text
                                                    *</label><input class="coral-Form-field cmp-teaser__editor-actionField"
                                                    data-cmp-teaser-v1-dialog-edit-hook="actionTitle" type="text" name="text"
                                                    placeholder="Text" value="" data-foundation-validation="" data-validation=""
                                                    is="coral-textfield"></div>
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
        const actionsEnabledCheckbox = {};
        const stubJQuery = sinon.stub(teaserConfig, '$');
        const jActionsEnabledCheckbox = {
            on: sinon.fake()
        };
        stubJQuery.withArgs(actionsEnabledCheckbox).returns(jActionsEnabledCheckbox);

        teaserConfig.actionsToggleHandler(actionsEnabledCheckbox);
        assert(jActionsEnabledCheckbox.on.calledOnceWith('change'));
    });

    it('handles pickers change', () => {
        const teaserConfig = new TeaserConfig(jQuery);
        const stubJQuery = sinon.stub(teaserConfig, '$');

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

        const eventData = { productField: 'product-field', categoryField: 'category-field' };

        stubJQuery
            .withArgs(document.querySelector(TeaserConfig.selectors.productFieldSelector))
            .returns(productElement);
        stubJQuery
            .withArgs(document.querySelector(TeaserConfig.selectors.categoryFieldSelector))
            .returns(categoryElement);

        const multiFieldActions = document.querySelector(TeaserConfig.selectors.actionsMultifieldSelector);
        teaserConfig.handlePickersChange(multiFieldActions);

        assert(productElement.off.calledOnceWithExactly('change', teaserConfig.handleProductChange));
        assert(categoryElement.off.calledOnceWithExactly('change', teaserConfig.handleCategoryChange));

        assert(productElement.adaptTo.calledOnceWithExactly('foundation-field'));
        assert(categoryElement.adaptTo.calledOnceWithExactly('foundation-field'));

        assert(productElement.on.calledOnceWithExactly('change', eventData, teaserConfig.handleProductChange));
        assert(categoryElement.on.calledOnceWithExactly('change', eventData, teaserConfig.handleCategoryChange));
    });

    it('handles product field change', () => {
        const teaserConfig = new TeaserConfig(jQuery);
        const productField = {
            getValue: sinon.fake.returns('')
        };
        const categoryField = {
            setValue: sinon.fake()
        };

        teaserConfig.handleProductChange({ data: { productField, categoryField } });
        assert(categoryField.setValue.notCalled);

        productField.getValue = sinon.fake.returns('test');
        teaserConfig.handleProductChange({ data: { productField, categoryField } });
        assert(categoryField.setValue.calledOnceWithExactly(''));
    });

    it('handles category field change', () => {
        const teaserConfig = new TeaserConfig(jQuery);
        const categoryField = {
            getValue: sinon.fake.returns('')
        };
        const productField = {
            setValue: sinon.fake()
        };

        teaserConfig.handleCategoryChange({ data: { productField, categoryField } });
        assert(productField.setValue.notCalled);

        categoryField.getValue = sinon.fake.returns('test');
        teaserConfig.handleCategoryChange({ data: { productField, categoryField } });
        assert(productField.setValue.calledOnceWithExactly(''));
    });
});
