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

class Navigation {
    constructor() {
        this.navigationPanel = document.querySelector(Navigation.selectors.navigationRoot);
        this.backNavigationButton = document.querySelector(Navigation.selectors.backNavigationButton);
        this.backNavigationEmpty = document.querySelector(Navigation.selectors.backNavigationEmpty);
        this.categoryTreeRoot = document.querySelector(Navigation.selectors.categoryTreeRoot);
        this.shadowTreeRoot = document.querySelector(Navigation.selectors.shadowTreeRoot);
        this.panelTitleElement = document.querySelector(Navigation.selectors.navigationHeaderTitle);
        this.navigationMask = document.querySelector(Navigation.selectors.navigationMask);

        if (!this.navigationPanel || !this.panelTitleElement) {
            return;
        }

        this.defaultPanelTitle = this.panelTitleElement.textContent;

        const backNavigationBinding = this.backNavigation.bind(this);
        const downNavigationBinding = this.downNavigation.bind(this);

        document
            .querySelector(Navigation.selectors.navigationTrigger)
            .addEventListener('click', () => this.showPanel());
        document
            .querySelector(Navigation.selectors.closeNavigationButton)
            .addEventListener('click', () => this.hidePanel());
        this.backNavigationButton.addEventListener('click', backNavigationBinding);
        document
            .querySelectorAll(Navigation.selectors.downNavigationButton)
            .forEach(a => a.addEventListener('click', downNavigationBinding));

        this.updateDynamicElements();

        // a flag that indicates that we're in the "navigation" view
        this.navigationPaneActive = true;

        this.navigationPanel.addEventListener('aem.accmg.start', () => {
            this.setVisible(this.backNavigationButton, true);
            this.setVisible(this.backNavigationEmpty, false);
        });

        this.navigationPanel.addEventListener('aem.accmg.step', ev => {
            if (ev.detail.title) {
                this.setPanelTitle(ev.detail.title);
            }
        });

        this.navigationPanel.addEventListener('aem.accmg.exit', () => {
            this.setPanelTitle(this.defaultPanelTitle);
            this.setVisible(this.backNavigationButton, false);
            this.setVisible(this.backNavigationEmpty, true);
        });

        this.hidePanel = this.hidePanel.bind(this);
        this.navigationMask.addEventListener('click', this.hidePanel);
    }

    setPanelTitle(title) {
        this.panelTitleElement.innerText = title;
    }

    showPanel() {
        this.navigationPanel.classList.add(Navigation.CSS_CLASS_NAVIGATION_OPEN);
        this.navigationMask.classList.add(Navigation.CSS_CLASS_MASK_ACTIVE);
    }

    hidePanel() {
        this.navigationPanel.classList.remove(Navigation.CSS_CLASS_NAVIGATION_OPEN);
        this.navigationMask.classList.remove(Navigation.CSS_CLASS_MASK_ACTIVE);
    }

    getActiveNavigation() {
        return document.querySelector(Navigation.selectors.activeNavigation);
    }

    updateDynamicElements() {
        // the back-navigation button is hidden for the root navigation and visible for all other navigations
        let activeNavigation = this.getActiveNavigation();
        let hasParent = activeNavigation && activeNavigation.dataset.parent;
        if (hasParent) {
            this.setVisible(this.backNavigationButton, true);
            this.setVisible(this.backNavigationEmpty, false);
        } else {
            this.setVisible(this.backNavigationButton, false);
            this.setVisible(this.backNavigationEmpty, true);
        }

        // the navigation item arrow is active if the navigation item has a child navigation with an active item
        let activeNavigationItem = document.querySelector(Navigation.selectors.activeNavigationItem);
        if (activeNavigationItem) {
            let id = this.getActiveNavigation().dataset.id;
            let activeChild = document.querySelector(
                Navigation.selectors.shadowNavigations + '[data-parent="' + id + '"] .cmp-navigation__item--active'
            );
            if (activeChild) {
                let activeNavigationItemArrow = document.querySelector(Navigation.selectors.activeNavigationItemArrow);
                if (
                    activeNavigationItemArrow &&
                    !activeNavigationItemArrow.classList.contains(Navigation.CSS_CLASS_ICON_ROOT_ACTIVE)
                ) {
                    activeNavigationItemArrow.classList.add(Navigation.CSS_CLASS_ICON_ROOT_ACTIVE);
                }
            }
        }
    }

    activateNavigation(id) {
        let navigation = document.querySelector(Navigation.selectors.shadowNavigations + '[data-id="' + id + '"]');
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
        var target = event.target;
        while (!target.classList.contains(Navigation.CSS_CLASS_CLICKABLE_ROOT)) {
            target = target.parentElement;
        }
        if (target) {
            this.activateNavigation(target.dataset.id);
        }
    }

    setVisible(element, visible) {
        if (visible) {
            element.style.display = 'block';
        } else {
            element.style.display = 'none';
        }
    }
}

Navigation.selectors = {
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
    downNavigationButton: '.categoryLeaf__root button',
    navigationMask: 'button.navigation__mask'
};

Navigation.CSS_CLASS_NAVIGATION_OPEN = 'navigation__root_open';
Navigation.CSS_CLASS_ICON_ROOT_ACTIVE = 'icon__root--active';
Navigation.CSS_CLASS_CLICKABLE_ROOT = 'clickable__root';
Navigation.CSS_CLASS_MASK_ACTIVE = 'navigation__mask_active';

(function() {
    function onDocumentReady() {
        new Navigation();
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export default Navigation;
