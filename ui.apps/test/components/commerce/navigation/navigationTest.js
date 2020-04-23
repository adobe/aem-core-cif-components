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

import Navigation from '../../../../src/main/content/jcr_root/apps/core/cif/components/structure/navigation/v1/navigation/clientlibs/js/navigation';

describe('Navigation', () => {
    let FIRST_ITEM_SELECTOR = '.header__primaryActions aside div.categoryTree__root li:nth-child(1)';
    var body;
    var navigationRoot;

    before(() => {
        window.CIF = {
            PageContext: {
                maskPage: function() {},
                unmaskPage: function() {}
            }
        };
        body = window.document.querySelector('body');
        navigationRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', navigationRoot);
        navigationRoot.insertAdjacentHTML(
            'afterbegin',
            `
                <div class="header__primaryActions">
                    <button class="navTrigger__root clickable__root" aria-label="Toggle navigation panel">
                        <span class="icon__root">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">                                
                            </svg>
                        </span>
                    </button>
                    <button data-role="mask" class="navigation__mask"></button>
                    <aside class="navigation__root navigation__root_open">                        
                        <div class="navigation__header">
                            <button class="trigger__root clickable__root trigger__root--back" type="button" style="display: none;">
                                <span class="icon__root">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
                                    </svg>
                                </span>
                            </button>
                            <h2 class="trigger__root trigger__root--back--empty" style="display: block;"></h2>
                            <h2 class="navHeader__title"><span>Main Menu</span></h2>
                            <button class="trigger__root clickable__root trigger__root--close" type="button">
                                <span class="icon__root">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
                                    </svg>
                                </span>
                            </button>
                        </div>                    
                        <nav class="navigation__body">
                            <div class="categoryTree__root">
                                <ul class="categoryTree__tree" data-id="ROOT_NAVIGATION">
                                    <li class="cmp-navigation__item cmp-navigation__item--level- cmp-navigation__item--active">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.34.html" title="Bottoms">
                                                    <span class="categoryLeaf__text">Bottoms</span>
                                                </a>
                                                <button class="trigger__root clickable__root" type="button" data-id="/content/atest/us/en/products/category-page.34.html">
                                                    <span class="icon__root">
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 30 30">
                                                        </svg>
                                                    </span>
                                                </button>
                                            </span>
                                        </span>
                                    </li>
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.31.html" title="Tops">
                                                    <span class="categoryLeaf__text">Tops</span>
                                                </a>
                                                <button class="trigger__root clickable__root" type="button" data-id="/content/atest/us/en/products/category-page.31.html">
                                                    <span class="icon__root">
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 30 30">
                                                        </svg>
                                                    </span>
                                                </button>
                                            </span>
                                        </span>
                                    </li>                                    
                                </ul>
                            </div>
                            <div class="categoryTree__root--shadow">
                                <ul class="categoryTree__tree" data-id="ROOT_NAVIGATION">
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.34.html" title="Bottoms">
                                                    <span class="categoryLeaf__text">Bottoms</span>
                                                </a>
                                                <button class="trigger__root clickable__root" type="button" data-id="/content/atest/us/en/products/category-page.34.html">
                                                    <span class="icon__root">
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 30 30">
                                                        </svg>
                                                    </span>
                                                </button>
                                            </span>
                                        </span>
                                    </li>
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.31.html" title="Tops">
                                                    <span class="categoryLeaf__text">Tops</span>
                                                </a>
                                                <button class="trigger__root clickable__root" type="button" data-id="/content/atest/us/en/products/category-page.31.html">
                                                    <span class="icon__root">
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 30 30">
                                                        </svg>
                                                    </span>
                                                </button>
                                            </span>
                                        </span>
                                    </li>
                                </ul>
                                <ul class="categoryTree__tree" data-parent="ROOT_NAVIGATION" data-id="/content/atest/us/en/products/category-page.34.html">
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.35.html" title="Pants &amp; Shorts">
                                                    <span class="categoryLeaf__text">Pants &amp; Shorts</span>
                                                </a>
                                            </span>
                                        </span>
                                    </li>
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.36.html" title="Skirts">
                                                    <span class="categoryLeaf__text">Skirts</span>
                                                </a>
                                            </span>
                                        </span>
                                    </li>
                                </ul>
                                <ul class="categoryTree__tree" data-parent="ROOT_NAVIGATION" data-id="/content/atest/us/en/products/category-page.31.html">
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.32.html" title="Blouses &amp; Shirts">
                                                    <span class="categoryLeaf__text">Blouses &amp; Shirts</span>
                                                </a>
                                            </span>
                                        </span>
                                    </li>
                                    <li class="cmp-navigation__item cmp-navigation__item--level-">
                                        <span class="categoryLeaf__root">
                                            <span class="categoryLeaf__root categoryLeaf__root--box">
                                                <a class="categoryLeaf__root categoryLeaf__root--link" href="/content/atest/us/en/products/category-page.33.html" title="Sweaters">
                                                    <span class="categoryLeaf__text">Sweaters</span>
                                                </a>
                                            </span>
                                        </span>
                                    </li>
                                </ul>
                            </div>
                        </nav>                        
                    </aside>
                </div>`
        );
    });

    after(() => {
        body.removeChild(navigationRoot);
    });

    it('initializes the Navigation component', () => {
        var navigation = new Navigation();

        assert.isNotNull(navigation.navigationPanel);
        assert.isNotNull(navigation.backNavigationButton);
        assert.isNotNull(navigation.backNavigationEmpty);
        assert.isNotNull(navigation.categoryTreeRoot);
        assert.isNotNull(navigation.shadowTreeRoot);
        assert.isNotNull(navigation.panelTitleElement);
        assert.equal('Main Menu', navigation.defaultPanelTitle);
        assert.isTrue(navigation.navigationPaneActive);
    });

    it('opens the navigation pane', () => {
        var navigation = new Navigation();

        document.querySelector(Navigation.selectors.closeNavigationButton).click();

        assert.isFalse(navigation.navigationPanel.classList.contains(Navigation.CSS_CLASS_NAVIGATION_OPEN));

        document.querySelector(Navigation.selectors.navigationTrigger).click();

        assert.isTrue(navigation.navigationPanel.classList.contains(Navigation.CSS_CLASS_NAVIGATION_OPEN));
        assert.equal('none', navigation.backNavigationButton.style.display);
        assert.equal('block', navigation.backNavigationEmpty.style.display);

        assert.isTrue(navigation.navigationMask.classList.contains(Navigation.CSS_CLASS_MASK_ACTIVE));
    });

    it('closes the navigation pane', () => {
        var navigation = new Navigation();

        document.querySelector(Navigation.selectors.navigationTrigger).click();

        assert.isTrue(navigation.navigationPanel.classList.contains(Navigation.CSS_CLASS_NAVIGATION_OPEN));

        document.querySelector(Navigation.selectors.closeNavigationButton).click();

        assert.isFalse(navigation.navigationPanel.classList.contains(Navigation.CSS_CLASS_NAVIGATION_OPEN));
        assert.isFalse(navigation.navigationMask.classList.contains(Navigation.CSS_CLASS_MASK_ACTIVE));
    });

    it('navigates down by button click', () => {
        var navigation = new Navigation();
        assert.equal('Bottoms', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);

        document.querySelector(FIRST_ITEM_SELECTOR + ' button').click();

        assert.equal('Pants &amp; Shorts', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);
        assert.equal('block', navigation.backNavigationButton.style.display);
        assert.equal('none', navigation.backNavigationEmpty.style.display);
    });

    it('navigates up one level', () => {
        var navigation = new Navigation();
        assert.equal('Pants &amp; Shorts', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);

        document.querySelector(Navigation.selectors.backNavigationButton).click();

        assert.equal('Bottoms', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);
        assert.equal('none', navigation.backNavigationButton.style.display);
        assert.equal('block', navigation.backNavigationEmpty.style.display);
    });

    it('navigates down by child element click', () => {
        var navigation = new Navigation();
        assert.equal('Bottoms', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);

        var target = document.querySelector(FIRST_ITEM_SELECTOR + ' button svg');

        let event = new CustomEvent('click');
        event.initCustomEvent('click', true, true);
        target.dispatchEvent(event);

        assert.equal('Pants &amp; Shorts', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);
        assert.equal('block', navigation.backNavigationButton.style.display);
        assert.equal('none', navigation.backNavigationEmpty.style.display);

        var targetLi = document.querySelector('.header__primaryActions aside div.categoryTree__root li:nth-child(1)');
        targetLi.classList.add('cmp-navigation__item--active');

        document.querySelector(Navigation.selectors.backNavigationButton).click();

        assert.equal('Bottoms', document.querySelector(FIRST_ITEM_SELECTOR + ' a span').innerHTML);
        assert.isTrue(document.querySelector(FIRST_ITEM_SELECTOR).classList.contains('cmp-navigation__item--active'));
        assert.equal('none', navigation.backNavigationButton.style.display);
        assert.equal('block', navigation.backNavigationEmpty.style.display);
    });

    it('honors aem.accmg.start event', () => {
        var navigation = new Navigation();
        var navigationPanel = document.querySelector(Navigation.selectors.navigationRoot);

        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.start'));

        assert.equal('block', navigation.backNavigationButton.style.display);
        assert.equal('none', navigation.backNavigationEmpty.style.display);
    });

    it('honors aem.accmg.step event', () => {
        var navigation = new Navigation();
        var navigationPanel = document.querySelector(Navigation.selectors.navigationRoot);

        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: {} }));

        assert.equal(navigation.panelTitleElement.textContent, navigation.defaultPanelTitle);

        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.step', { detail: { title: 'A Title' } }));

        assert.equal(navigation.panelTitleElement.textContent, 'A Title');
    });

    it('honors aem.accmg.exit event', () => {
        var navigation = new Navigation();
        var navigationPanel = document.querySelector(Navigation.selectors.navigationRoot);

        navigationPanel.dispatchEvent(new CustomEvent('aem.accmg.exit'));

        assert.equal('none', navigation.backNavigationButton.style.display);
        assert.equal('block', navigation.backNavigationEmpty.style.display);
        assert.equal(navigation.panelTitleElement.textContent, navigation.defaultPanelTitle);
    });

    it('honors empty navigation', () => {
        var categoryTree = window.document.querySelector('.categoryTree__root');
        var categoryTreeShadow = window.document.querySelector('.categoryTree__root--shadow');

        // empty navigation
        categoryTree.innerHTML = '';
        categoryTreeShadow.innerHTML = '';

        var navigation = new Navigation();

        assert.equal('none', navigation.backNavigationButton.style.display);
        assert.equal('block', navigation.backNavigationEmpty.style.display);
        assert.equal(navigation.panelTitleElement.textContent, navigation.defaultPanelTitle);
    });
});
