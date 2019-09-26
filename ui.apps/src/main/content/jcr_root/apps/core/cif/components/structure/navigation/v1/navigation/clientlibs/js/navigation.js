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

(function() {
    'use strict';

    const selectors = {
        navigationHeaderTitle: '.navigation__header .navHeader__title',
        navigationTrigger: '.header__primaryActions .navTrigger__root',
        navigationRoot: 'aside.navigation__root',
        shadowTreeRoot: '.categoryTree__root--shadow',
        categoryTreeRoot: '.categoryTree__root',
        activeNavigation: '.categoryTree__root .categoryTree__tree',
        activeNavigationItem: '.categoryTree__root .categoryTree__tree .cmp-navigation__item--active',
        activeNavigationItemArrow: '.categoryTree__root .categoryTree__tree .cmp-navigation__item--active .icon__root',
        shadowNavigations: '.categoryTree__root--shadow .categoryTree__tree',
        backNavigationButton: '.navigation__header .trigger__root--back',
        backNavigationEmpty: '.navigation__header .trigger__root--back--empty',
        closeNavigationButton: '.navigation__header .trigger__root--close',
        downNavigationButton: '.categoryLeaf__root button'
    };

    const CSS_CLASS_NAVIGATION_OPEN = 'navigation__root_open';
    const CSS_CLASS_ICON_ROOT_ACTIVE = 'icon__root--active';

    class Navigation {
        constructor() {
            this.navigationPanel = document.querySelector(selectors.navigationRoot);
            this.backNavigationButton = document.querySelector(selectors.backNavigationButton);
            this.backNavigationEmpty = document.querySelector(selectors.backNavigationEmpty);
            this.categoryTreeRoot = document.querySelector(selectors.categoryTreeRoot);
            this.shadowTreeRoot = document.querySelector(selectors.shadowTreeRoot);

            const backNavigationBinding = this.backNavigation.bind(this);
            const downNavigationBinding = this.downNavigation.bind(this);

            document.querySelector(selectors.navigationTrigger).addEventListener('click', () => this.showPanel());
            document.querySelector(selectors.closeNavigationButton).addEventListener('click', () => this.hidePanel());
            this.backNavigationButton.addEventListener('click', backNavigationBinding);
            document
                .querySelectorAll(selectors.downNavigationButton)
                .forEach(a => a.addEventListener('click', downNavigationBinding));

            this.updateDynamicElements();

            // a flag that indicates that we're in the "navigation" view
            this.navigationPaneActive = true;

            this.navigationPanel.addEventListener('aem.accmg.start', (ev)=>{
                this.setVisible(this.backNavigationButton, true);
                this.setVisible(this.backNavigationEmpty, false);
            });

        }

        showPanel() {
            this.navigationPanel.classList.add(CSS_CLASS_NAVIGATION_OPEN);
            window.CIF.PageContext.maskPage(this.hidePanel.bind(this));
        }

        hidePanel() {
            this.navigationPanel.classList.remove(CSS_CLASS_NAVIGATION_OPEN);
            window.CIF.PageContext.unmaskPage();
        }

        getActiveNavigation() {
            return document.querySelector(selectors.activeNavigation);
        }

        updateDynamicElements() {
            // the back-navigation button is hidden for the root navigation and visible for all other navigations
            let hasParent = this.getActiveNavigation().dataset.parent;
            if (hasParent) {
                this.setVisible(this.backNavigationButton, true);
                this.setVisible(this.backNavigationEmpty, false);
            } else {
                this.setVisible(this.backNavigationButton, false);
                this.setVisible(this.backNavigationEmpty, true);
            }

            // the navigation item arrow is active if the navigation item has a child navigation with an active item
            let activeNavigationItem = document.querySelector(selectors.activeNavigationItem);
            if (activeNavigationItem) {
                let id = this.getActiveNavigation().dataset.id;
                let activeChild = document.querySelector(
                    selectors.shadowNavigations + '[data-parent="' + id + '"] .cmp-navigation__item--active'
                );
                if (activeChild) {
                    let activeNavigationItemArrow = document.querySelector(selectors.activeNavigationItemArrow);
                    if (
                        activeNavigationItemArrow &&
                        !activeNavigationItemArrow.classList.contains(CSS_CLASS_ICON_ROOT_ACTIVE)
                    ) {
                        activeNavigationItemArrow.classList.add(CSS_CLASS_ICON_ROOT_ACTIVE);
                    }
                }
            }
        }

        activateNavigation(id) {
            let navigation = document.querySelector(selectors.shadowNavigations + '[data-id="' + id + '"]');
            if (navigation) {
                this.shadowTreeRoot.append(this.getActiveNavigation());
                this.categoryTreeRoot.append(navigation);

                this.updateDynamicElements();
            }
        }

        backNavigation() {
            let id = this.getActiveNavigation().dataset.parent;
            this.activateNavigation(id);

            const event = new CustomEvent('aem.navigation.back');
            document.dispatchEvent(event);
            
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

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();
