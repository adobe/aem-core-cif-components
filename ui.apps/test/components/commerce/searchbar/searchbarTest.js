/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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

import Searchbar from '../../../../src/main/content/jcr_root/apps/core/cif/components/commerce/searchbar/v2/searchbar/clientlibs/js/searchbar';

describe('Searchbar', () => {
    var body;
    var searchbarRoot;

    before(() => {
        body = window.document.querySelector('body');
        searchbarRoot = document.createElement('div');
        body.insertAdjacentElement('afterbegin', searchbarRoot);
        searchbarRoot.insertAdjacentHTML(
            'afterbegin',
            `
                <div class="searchbar__root" id="searchbar-df5a1ddbdd">
                    <button class="searchbar__trigger" title="Search">
                        <span class="searchbar__trigger-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="11" cy="11" r="8"></circle>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                            </svg>
                        </span>
                    </button>
                    <div class="searchbar__body" role="search">
                        <div class="searchbar__form-container">
                            <form class="searchbar__form" autocomplete="off" action="/content/venia/us/en/search.html">
                                <span class="searchbar__fields" style="--iconsBefore:1; --iconsAfter:0;">
                                    <span class="searchbar__input-container">
                                        <input class="searchbar__input" name="search_query" value="" role="searchbox" placeholder="Search">
                                    </span>
                                    <span class="searchbar__input-before">
                                        <span class="searchbar__search-icon">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                <circle cx="11" cy="11" r="8"></circle>
                                                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                            </svg>
                                        </span>
                                    </span>
                                    <span class="searchbar__input-after"></span>
                                </span>
                            </form>
                        </div>
                    </div>
                </div>`
        );
    });

    after(() => {
        body.removeChild(searchbarRoot);
    });

    it('initializes the Searchbar component', () => {
        const searchbar = new Searchbar();

        assert.isNotNull(searchbar._searchBarRoot);
        assert.isNotNull(searchbar._searchBox);
        assert.isNotNull(searchbar._state);
        assert.isUndefined(searchbar._state.visible);
        assert.isFalse(searchbar._searchBarRoot.classList.contains(searchbar._classes.open));
    });

    it('opens the searchbar', () => {
        const searchbar = new Searchbar();
        const searchBarToggle = document.querySelector(Searchbar.selectors.searchBarToggle);
        searchBarToggle.click();

        assert.isTrue(searchbar._state.visible);
        assert.isTrue(searchbar._searchBarRoot.classList.contains(searchbar._classes.open));
    });

    it('displays and hides the reset button', () => {
        const searchbar = new Searchbar();
        const searchBarToggle = document.querySelector(Searchbar.selectors.searchBarToggle);
        searchBarToggle.click();

        let reset = document.querySelector('.searchbar__reset-button');
        assert.isNull(reset);

        searchbar._searchBox.dispatchEvent(new KeyboardEvent('keydown', { key: 'e' }));

        reset = document.querySelector('.searchbar__reset-button');
        assert.isNotNull(reset);
        searchbar._searchBox.value = 'e';

        reset.click();
        // field is empty
        assert.isEmpty(searchbar._searchBox.value);
        reset = document.querySelector('.searchbar__reset-button');
        // reset button removed
        assert.isNull(reset);
    });

    it('closes the searchbar', () => {
        const searchbar = new Searchbar();
        const searchBarToggle = document.querySelector(Searchbar.selectors.searchBarToggle);
        // show
        searchBarToggle.click();
        // hide
        searchBarToggle.click();

        assert.isFalse(searchbar._state.visible);
        assert.isFalse(searchbar._searchBarRoot.classList.contains(searchbar._classes.open));
    });

    it('honors the query parameter', () => {
        const searchbar = new Searchbar({ params: { query: 'dress' } });

        // if query parameter is present then the searchbar is open from the start
        assert.isTrue(searchbar._state.visible);
        assert.isTrue(searchbar._searchBarRoot.classList.contains(searchbar._classes.open));
    });
});
