/*******************************************************************************
 * Copyright 2021 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 This client library is adapted from https://github.com/adobe/aem-core-wcm-components/blob/master/content/src/content/jcr_root/apps/core/wcm/components/contentfragment/v1/contentfragment/clientlibs/editor/dialog/js/editDialog.js
 We may need to update this client library as the above client library evolves.
 */
(function(window, $, channel, Granite, Coral) {
    "use strict";

    // class of the edit dialog content
    var CLASS_EDIT_DIALOG = "cmp-contentfragment__editor";

    // field selectors
    var SELECTOR_MODEL_PATH = "[name='./modelPath']";
    var SELECTOR_LINK_ELEMENT = "[name='./linkElement']";
    var SELECTOR_ELEMENT_NAMES_CONTAINER = "[data-element-names-container='true']";
    var SELECTOR_ELEMENT_NAMES = "[data-granite-coral-multifield-name='./elementNames']";
    var SELECTOR_SINGLE_TEXT_ELEMENT = "[data-single-text-selector='true']";
    var SELECTOR_ELEMENT_NAMES_ADD = SELECTOR_ELEMENT_NAMES + " > [is=coral-button]";
    var SELECTOR_DISPLAY_MODE_RADIO_GROUP = "[data-display-mode-radio-group='true']";
    var SELECTOR_DISPLAY_MODE_CHECKED = "[name='./displayMode']:checked";
    var SELECTOR_PARAGRAPH_CONTROLS = ".cmp-contentfragment__editor-paragraph-controls";
    var SELECTOR_PARAGRAPH_SCOPE = "[name='./paragraphScope']";
    var SELECTOR_PARAGRAPH_RANGE = "[name='./paragraphRange']";
    var SELECTOR_PARAGRAPH_HEADINGS = "[name='./paragraphHeadings']";

    // mode in which only one multiline text element could be selected for display
    var SINGLE_TEXT_DISPLAY_MODE = "singleText";

    // ui helper
    var ui = $(window).adaptTo("foundation-ui");

    // dialog texts
    var confirmationDialogTitle = Granite.I18n.get("Warning");
    var confirmationDialogMessage = Granite.I18n.get("Please confirm replacing the current content fragment model configuration");
    var confirmationDialogCancel = Granite.I18n.get("Cancel");
    var confirmationDialogConfirm = Granite.I18n.get("Confirm");
    var errorDialogTitle = Granite.I18n.get("Error");
    var errorDialogMessage = Granite.I18n.get("Failed to load the elements of the selected content fragment model");

    // the model path field
    var modelPath;

    // the paragraph controls (field set)
    var paragraphControls;
    // the tab containing paragraph control
    var paragraphControlsTab;

    // keeps track of the current model path
    var currentModelPath;

    var editDialog;

    var elementsController;

    /**
     * A class which encapsulates the logic related to element selectors.
     */
    var ElementsController = function() {
        // container which contains either single elements select field or a multifield of element selectors
        this.elementNamesContainer = editDialog.querySelector(SELECTOR_ELEMENT_NAMES_CONTAINER);
        // element container resource path
        this.elementsContainerPath = this.elementNamesContainer.dataset.fieldPath;
        // link element resource path
        this.linkElements = editDialog.querySelector(SELECTOR_LINK_ELEMENT);
        // link element container resource path
        this.linkElementsPath = this.linkElements.dataset.fieldPath;
        this.fetchedState = null;
        this._updateFields();
    };

    /**
     * Updates the member fields of this class according to current dom of dialog.
     */
    ElementsController.prototype._updateFields = function() {
        // The multifield containing element selector dropdowns
        this.elementNames = editDialog.querySelector(SELECTOR_ELEMENT_NAMES);
        // link element resource path
        this.linkElements = editDialog.querySelector(SELECTOR_LINK_ELEMENT);
        // The add button in multifield
        this.addElement = this.elementNames ? this.elementNames.querySelector(SELECTOR_ELEMENT_NAMES_ADD) : undefined;
        // The dropdown containing element selector for multiline text elements only. Either this or the elementNames
        // multifield should be visible to user at a time.
        this.singleTextSelector = editDialog.querySelector(SELECTOR_SINGLE_TEXT_ELEMENT);
    };

    /**
     * Disable all the fields of this controller.
     */
    ElementsController.prototype.disableFields = function() {
        if (this.addElement) {
            this.addElement.setAttribute("disabled", "");
        }
        if (this.singleTextSelector) {
            this.singleTextSelector.setAttribute("disabled", "");
        }
        if (this.linkElements) {
            this.linkElements.setAttribute("disabled", "");
        }
    };

    /**
     * Enable all the fields of this controller.
     */
    ElementsController.prototype.enableFields = function() {
        if (this.addElement) {
            this.addElement.removeAttribute("disabled");
        }
        if (this.singleTextSelector) {
            this.singleTextSelector.removeAttribute("disabled");
        }
        if (this.linkElements) {
            this.linkElements.removeAttribute("disabled");
        }
    };

    /**
     * Resets all the fields of this controller.
     */
    ElementsController.prototype.resetFields = function() {
        if (this.elementNames) {
            this.elementNames.items.clear();
            this.elementNames.value = null;
        }
        if (this.singleTextSelector) {
            this.singleTextSelector.value = "";
        }
        if (this.linkElements) {
            this.linkElements.items.clear();
            this.linkElements.value = null;
        }

        let a = null;
        // alert(a.b);
    };

    /**
     * Creates an http request object for retrieving model's element names and returns it.
     *
     * @param {String} displayMode - displayMode parameter for element name request. Should be "singleText" or "multi"
     * @param {String} type - type of request. It can have the following values -
     * 1. "linkElements" for getting link element names
     * 2. "elements" for getting element names
     * @returns {Object} the resulting request
     */
    ElementsController.prototype.prepareRequest = function(displayMode, type) {
        if (typeof displayMode === "undefined") {
            displayMode = editDialog.querySelector(SELECTOR_DISPLAY_MODE_CHECKED).value;
        }
        var data = {
            modelPath: modelPath.value,
            displayMode: displayMode
        };
        var url;
        if (type === "linkElements") {
            url = Granite.HTTP.externalize(this.linkElementsPath) + ".html";
        } else if (type === "elements") {
            url = Granite.HTTP.externalize(this.elementsContainerPath) + ".html";
        }
        var request = $.get({
            url: url,
            data: data
        });
        return request;
    };

    /**
     * Retrieves the html for element names and link element names and keeps the fetched values as "fetchedState" member.
     *
     * @param {String} displayMode - display mode to use as parameter of element names request
     * @param {Function} callback - function to execute when response is received
     */
    ElementsController.prototype.testGetHTML = function(displayMode, callback) {
        var elementNamesRequest = this.prepareRequest(displayMode, "elements");
        var linkElementsRequest = this.prepareRequest(displayMode, "linkElements");
        var self = this;
        // wait for requests to load
        $.when(elementNamesRequest, linkElementsRequest).then(function(result1, result2) {
            var newElementNames = $(result1[0]).find(SELECTOR_ELEMENT_NAMES)[0];
            var newSingleTextSelector = $(result1[0]).find(SELECTOR_SINGLE_TEXT_ELEMENT)[0];
            var newLinkElement = $(result2[0]).find(SELECTOR_LINK_ELEMENT)[0];
            // get the fields from the resulting markup and create a test state
            Coral.commons.ready(newElementNames, function() {
                Coral.commons.ready(newSingleTextSelector, function() {
                    Coral.commons.ready(newLinkElement, function() {
                        self.fetchedState = {
                            elementNames: newElementNames,
                            singleTextSelector: newSingleTextSelector,
                            linkElement: newLinkElement,
                            elementNamesContainerHTML: result1[0],
                        };
                        callback();
                    });
                });
            });

        }, function() {
            // display error dialog if one of the requests failed
            ui.prompt(errorDialogTitle, errorDialogMessage, "error");
        });
    };

    /**
     * Checks if the current states of element names, single text selector and link element names match with those
     * present in fetchedState.
     *
     * @returns {Boolean} true if the states match or if there was no current state, false otherwise
     */
    ElementsController.prototype.testStateForUpdate = function() {
        // check if some element names are currently configured
        if (this.elementNames && this.elementNames.items.length > 0) {
            return false;
        }

        if (this.singleTextSelector && this.singleTextSelector.items.length > 0) {
            return false;
        }

        // check if a link element is currently configured
        if (this.linkElements.value) {
            return false;
        }

        return true;
    };

    /**
     * Replace the current state with the values present in fetchedState and discard the fetchedState thereafter.
     */
    ElementsController.prototype.saveFetchedState = function() {
        if (!this.fetchedState) {
            return;
        }
        if (!this.elementNames && !this.singleTextSelector) {
            this._updateElementsHTML(this.fetchedState.elementNamesContainerHTML);
        } else if (this.fetchedState.elementNames) {
            if (this.fetchedState.elementNames.template.content.children) {
                this._updateElementsDOM(this.fetchedState.elementNames);
            } else {
                // if the content of template is not accessible through the DOM (IE 11!),
                // then use the HTML to update the elements multifield
                this._updateElementsHTML(this.fetchedState.elementNamesContainerHTML);
            }
        } else {
            this._updateElementsDOM(this.fetchedState.singleTextSelector);
        }
        this._updateLinkElementsDOM(this.fetchedState.linkElement);
        this.discardFetchedState();
    };

    /**
     * Discard the fetchedState data.
     */
    ElementsController.prototype.discardFetchedState = function() {
        this.fetchedState = null;
    };

    /**
     * Retrieve element names and update the current element names with the retrieved data.
     *
     * @param {String} displayMode - selected display mode of the component
     */
    ElementsController.prototype.fetchAndUpdateElementsHTML = function(displayMode) {
        var elementNamesRequest = this.prepareRequest(displayMode, "elements");
        var self = this;
        // wait for requests to load
        $.when(elementNamesRequest).then(function(result) {
            self._updateElementsHTML(result);
        }, function() {
            // display error dialog if one of the requests failed
            ui.prompt(errorDialogTitle, errorDialogMessage, "error");
        });
    };

    /**
     * Updates inner html of element container.
     *
     * @param {String} html - outerHTML value for elementNamesContainer
     */
    ElementsController.prototype._updateElementsHTML = function(html) {
        this.elementNamesContainer.innerHTML = $(html)[0].innerHTML;
        this._updateFields();
    };

    /**
     * Updates dom of element container with the passed dom. If the passed dom is multifield, the current multifield
     * template would be replaced with the dom's template otherwise the dom would used as the new singleTextSelector
     * member.
     *
     * @param {HTMLElement} dom - new dom
     */
    ElementsController.prototype._updateElementsDOM = function(dom) {
        if (dom.tagName === "CORAL-MULTIFIELD") {
            // replace the element names multifield's template
            this.elementNames.template = dom.template;
        } else {
            dom.value = this.singleTextSelector.value;
            this.singleTextSelector.parentNode.replaceChild(dom, this.singleTextSelector);
            this.singleTextSelector = dom;
            this.singleTextSelector.removeAttribute("disabled");
        }
        this._updateFields();
    };

    /**
     * Updates dom of link element select dropdown.
     *
     * @param {HTMLElement} dom - dom for link element dropdown
     */
    ElementsController.prototype._updateLinkElementsDOM = function(dom) {
        // replace the linkElements select, keeping its value
        dom.value = this.linkElements.value;
        this.linkElements.parentNode.replaceChild(dom, this.linkElements);
        this.linkElements = dom;
        this.linkElements.removeAttribute("disabled");
        this._updateFields();
    };

    function initialize(dialog) {
        // get path of component being edited
        editDialog = dialog;

        // get the fields
        modelPath = dialog.querySelector(SELECTOR_MODEL_PATH);
        paragraphControls = dialog.querySelector(SELECTOR_PARAGRAPH_CONTROLS);
        paragraphControlsTab = dialog.querySelector("coral-tabview").tabList.items.getAll()[1];

        // initialize state variables
        currentModelPath = modelPath.value;
        elementsController = new ElementsController();

        // disable add button and link element if no model is currently set
        if (!currentModelPath) {
            elementsController.disableFields();
        }
        // enable / disable the paragraph controls
        setParagraphControlsState();
        // hide/show paragraph control tab
        updateParagraphControlTabState();

        // register change listener
        $(modelPath).on("foundation-field-change", onModelPathChange);
        $(document).on("change", SELECTOR_PARAGRAPH_SCOPE, setParagraphControlsState);
        var $radioGroup = $(dialog).find(SELECTOR_DISPLAY_MODE_RADIO_GROUP);
        $radioGroup.on("change", function(e) {
            elementsController.fetchAndUpdateElementsHTML(e.target.value);
            updateParagraphControlTabState();
        });
    }

    /**
     * Executes after the model path has changed. Shows a confirmation dialog to the user if the current
     * configuration is to be reset and updates the fields to reflect the newly selected model.
     */
    function onModelPathChange() {
        // if the model was reset (i.e. the model path was deleted)
        if (!modelPath.value) {
            var canKeepConfig = elementsController.testStateForUpdate();
            if (canKeepConfig) {
                // There was no current configuration. We just need to disable fields.
                currentModelPath = modelPath.value;
                elementsController.disableFields();
                return;
            }
            // There was some current configuration. Show a confirmation dialog
            confirmModelChange(null, null, elementsController.disableFields, elementsController);
            // don't do anything else
            return;
        }

        elementsController.testGetHTML(editDialog.querySelector(SELECTOR_DISPLAY_MODE_CHECKED).value, function() {
            // check if we can keep the current configuration, in which case no confirmation dialog is necessary
            var canKeepConfig = elementsController.testStateForUpdate();
            if (canKeepConfig) {
                if (!currentModelPath) {
                    elementsController.enableFields();
                }
                currentModelPath = modelPath.value;
                // its okay to save fetched state
                elementsController.saveFetchedState();
                return;
            }
            // else show a confirmation dialog
            confirmModelChange(elementsController.discardFetchedState, elementsController,
                elementsController.saveFetchedState, elementsController);
        });
    }

    /**
     * Presents the user with a confirmation dialog if the current configuration needs to be reset as a result
     * of the content model change.
     *
     * @param {Function} cancelCallback - callback to call if change is cancelled
     * @param {Object} cancelCallbackScope - scope (value of "this" keyword) for cancelCallback
     * @param {Function} confirmCallback - a callback to execute after the change is confirmed
     * @param {Object} confirmCallbackScope - the scope (value of "this" keyword) to use for confirmCallback
     */
    function confirmModelChange(cancelCallback, cancelCallbackScope, confirmCallback, confirmCallbackScope) {

        ui.prompt(confirmationDialogTitle, confirmationDialogMessage, "warning", [{
            text: confirmationDialogCancel,
            handler: function() {
                // reset the model path to its previous value
                requestAnimationFrame(function() {
                    modelPath.value = currentModelPath;
                });
                if (cancelCallback) {
                    cancelCallback.call(cancelCallbackScope);
                }
            }
        }, {
            text: confirmationDialogConfirm,
            primary: true,
            handler: function() {
                // reset the current configuration
                elementsController.resetFields();
                // update the current model path
                currentModelPath = modelPath.value;
                // execute callback
                if (confirmCallback) {
                    confirmCallback.call(confirmCallbackScope);
                }
            }
        }]);
    }

    /**
     * Enables or disables the paragraph range and headings field depending on the state of the paragraph scope field.
     */
    function setParagraphControlsState() {
        // get the selected scope radio button (might not be present at all)
        var scope = paragraphControls.querySelector(SELECTOR_PARAGRAPH_SCOPE + "[checked]");
        if (scope) {
            // enable or disable range and headings fields according to the scope value
            var range = paragraphControls.querySelector(SELECTOR_PARAGRAPH_RANGE);
            var headings = paragraphControls.querySelector(SELECTOR_PARAGRAPH_HEADINGS);
            if (scope.value === "range") {
                range.removeAttribute("disabled");
                headings.removeAttribute("disabled");
            } else {
                range.setAttribute("disabled", "");
                headings.setAttribute("disabled", "");
            }
        }
    }

    // Toggles the display of paragraph control tab depending on display mode
    function updateParagraphControlTabState() {
        var displayMode = editDialog.querySelector(SELECTOR_DISPLAY_MODE_CHECKED).value;
        if (displayMode === SINGLE_TEXT_DISPLAY_MODE) {
            paragraphControlsTab.hidden = false;
        } else {
            paragraphControlsTab.hidden = true;
        }
    }

    /**
     * Initializes the dialog after it has loaded.
     */
    channel.on("foundation-contentloaded", function(e) {
        if (e.target.getElementsByClassName(CLASS_EDIT_DIALOG).length > 0) {
            Coral.commons.ready(e.target, function(dialog) {
                initialize(dialog);
            });
        }
    });

})(window, jQuery, jQuery(document), Granite, Coral);
