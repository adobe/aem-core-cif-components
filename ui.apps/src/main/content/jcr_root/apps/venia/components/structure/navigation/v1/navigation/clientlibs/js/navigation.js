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

(function () {
    "use strict";
    const selectors = {
        navigationTrigger: '.header__primaryActions .navTrigger__root',
        navigationRoot: 'aside.navigation__root',
        shadowTreeRoot: '.categoryTree__root--shadow',
        categoryTreeRoot: '.categoryTree__root',
        activeNavigation: '.categoryTree__root .categoryTree__tree',
        shadowNavigations: '.categoryTree__root--shadow .categoryTree__tree',
        backNavigationButton: '.navigation__header .trigger__root--back',
        backNavigationEmpty: '.navigation__header .trigger__root--back--empty',
        closeNavigationButton: '.navigation__header .trigger__root--close',
        downNavigationButton: '.categoryLeaf__root button'
    };

    const CSS_NAVIGATION_OPEN = 'navigation__root_open';

    let backNavigationButton;
    let backNavigationEmpty;
    let categoryTreeRoot;
    let shadowTreeRoot;
    let navigationPanel;

    function setNavigationPanelVisible(visible) {
        if (visible) {
            navigationPanel.classList.add(CSS_NAVIGATION_OPEN);
        } else {
            navigationPanel.classList.remove(CSS_NAVIGATION_OPEN);
        }
    }

    function setVisible(element, visible) {
        if (visible) {
            element.style.display = 'block';
        } else {
            element.style.display = 'none';
        }
    }

    function getActiveNavigation() {
        return document.querySelector(selectors.activeNavigation);
    }

    function configureBackNavigation() {
        let hasParent = getActiveNavigation().dataset.parent;
        if (hasParent) {
            setVisible(backNavigationButton, true);
            setVisible(backNavigationEmpty, false);
        } else {
            setVisible(backNavigationButton, false);
            setVisible(backNavigationEmpty, true);
        }
    }

    function activateNavigation(id) {
        let navigation = document.querySelector(selectors.shadowNavigations + '[data-id="' + id + '"]');
        if (navigation) {
            shadowTreeRoot.append(getActiveNavigation());
            categoryTreeRoot.append(navigation);

            configureBackNavigation();
        }
    }

    function backNavigation() {
        let id = getActiveNavigation().dataset.parent;
        activateNavigation(id);
    }

    function downNavigation(event) {
        const id = event.target.parentElement.parentElement.dataset.id;
        activateNavigation(id);
    }

    function onDocumentReady() {
        navigationPanel = document.querySelector(selectors.navigationRoot);
        backNavigationButton = document.querySelector(selectors.backNavigationButton);
        backNavigationEmpty = document.querySelector(selectors.backNavigationEmpty);
        categoryTreeRoot = document.querySelector(selectors.categoryTreeRoot);
        shadowTreeRoot = document.querySelector(selectors.shadowTreeRoot);

        document.querySelector(selectors.navigationTrigger).addEventListener('click', () => setNavigationPanelVisible(true));
        document.querySelector(selectors.closeNavigationButton).addEventListener('click', () => setNavigationPanelVisible(false));
        backNavigationButton.addEventListener('click', backNavigation);
        document.querySelectorAll(selectors.downNavigationButton).forEach((a) => {a.addEventListener('click', downNavigation)});

        configureBackNavigation();
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }
    
})();