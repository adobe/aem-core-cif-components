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

    class Navigation {

        constructor() {
            this.navigationPanel = document.querySelector(selectors.navigationRoot);
            this.backNavigationButton = document.querySelector(selectors.backNavigationButton);
            this.backNavigationEmpty = document.querySelector(selectors.backNavigationEmpty);
            this.categoryTreeRoot = document.querySelector(selectors.categoryTreeRoot);
            this.shadowTreeRoot = document.querySelector(selectors.shadowTreeRoot);

            const backNavigationBinding = this.backNavigation.bind(this);
            const downNavigationBinding = this.downNavigation.bind(this);

            document.querySelector(selectors.navigationTrigger).addEventListener('click', () => this.setNavigationPanelVisible(true));
            document.querySelector(selectors.closeNavigationButton).addEventListener('click', () => this.setNavigationPanelVisible(false));
            this.backNavigationButton.addEventListener('click', backNavigationBinding);
            document.querySelectorAll(selectors.downNavigationButton).forEach((a) => a.addEventListener('click', downNavigationBinding));

            this.configureBackNavigation();
        }

        setNavigationPanelVisible(visible) {
            if (visible) {
                this.navigationPanel.classList.add(CSS_NAVIGATION_OPEN);
            } else {
                this.navigationPanel.classList.remove(CSS_NAVIGATION_OPEN);
            }
        }

        getActiveNavigation() {
            return document.querySelector(selectors.activeNavigation);
        }

        configureBackNavigation() {
            let hasParent = this.getActiveNavigation().dataset.parent;
            if (hasParent) {
                this.setVisible(this.backNavigationButton, true);
                this.setVisible(this.backNavigationEmpty, false);
            } else {
                this.setVisible(this.backNavigationButton, false);
                this.setVisible(this.backNavigationEmpty, true);
            }
        }

        activateNavigation(id) {
            let navigation = document.querySelector(selectors.shadowNavigations + '[data-id="' + id + '"]');
            if (navigation) {
                this.shadowTreeRoot.append(this.getActiveNavigation());
                this.categoryTreeRoot.append(navigation);

                this.configureBackNavigation();
            }
        }

        backNavigation() {
            let id = this.getActiveNavigation().dataset.parent;
            this.activateNavigation(id);
        }

        downNavigation(event) {
            let id = event.target.parentElement.parentElement.dataset.id;
            this.activateNavigation(id);
        }

        setVisible(element, visible) {
            if (visible) {
                element.style.display = 'block';
            } else {
                element.style.display = 'none';
            }
        }
    }

    function onDocumentReady() {
        new Navigation();
    }

    if (document.readyState !== "loading") {
        onDocumentReady();
    } else {
        document.addEventListener("DOMContentLoaded", onDocumentReady);
    }

})();