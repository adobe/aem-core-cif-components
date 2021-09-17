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

class Searchbar {
    constructor(props) {
        this._classes = {
            open: 'searchbar__body--open'
        };

        this._searchBarRoot = document.querySelector(Searchbar.selectors.searchBarRoot);
        if (!this._searchBarRoot) {
            return;
        }

        this._searchBox = this._searchBarRoot.querySelector(Searchbar.selectors.searchBox);

        let stateObject = {};
        if (props && props.params) {
            stateObject.visible = !!props.params.query;
            stateObject.query = props.params.query;
        }

        this._state = stateObject;

        if (stateObject.visible) {
            this._show();
            this._searchBox.value = stateObject.query;
            this._showResetButton();
        }

        this._installListeners();
    }

    _getOrCreateResetButton() {
        if (!this._resetButtonElement) {
            const html = new DOMParser().parseFromString(Searchbar.CLEAR_BUTTON_HTML, 'text/html');
            this._resetButtonElement = html.body.firstChild;
        }
        return this._resetButtonElement;
    }

    _installListeners() {
        // listen to onclick on the "search" icon in the header
        document.querySelector(Searchbar.selectors.searchBarToggle).addEventListener('click', event => {
            this.toggle();
        });
        // initial registration of search box listener for use cases when the searchbar toggle is not used
        this._registerSearchBoxListener();
    }

    toggle() {
        if (this._state.visible) {
            this._hide();
        } else {
            this._show();
        }

        this._state.visible = !this._state.visible;
    }

    _showResetButton() {
        const afterField = this._searchBarRoot.querySelector(Searchbar.selectors.afterField);
        afterField.appendChild(this._getOrCreateResetButton());

        this._searchBarRoot.querySelector(Searchbar.selectors.resetButton).addEventListener('click', e => {
            //clear the search field
            this._searchBox.value = '';

            //remove the reset button if exists
            if (afterField.childNodes.length > 0) {
                afterField.removeChild(afterField.childNodes[0]);
            }

            //re-register the listener on the searchbox
            this._registerSearchBoxListener();
            this._searchBox.focus();
        });
    }

    _registerSearchBoxListener() {
        const _handleKeyDown = e => {
            this._showResetButton();
            this._searchBox.removeEventListener('keydown', _handleKeyDown);
        };
        this._searchBox.addEventListener('keydown', _handleKeyDown);
    }

    _show() {
        this._searchBarRoot.classList.add(this._classes.open);
        this._searchBox.focus();
        this._registerSearchBoxListener();
    }

    _hide() {
        this._searchBarRoot.classList.remove(this._classes.open);
    }
}

Searchbar.selectors = {
    searchBarRoot: "div[role='search']",
    searchBox: "input[role='searchbox']",
    afterField: '.searchbar__input-after',
    resetButton: '.searchbar__reset-button',
    searchBarToggle: '.searchbar__trigger'
};

Searchbar.CLEAR_BUTTON_HTML = `
    <button class="searchbar__reset-button" type="button">
        <span class="icon__root">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        </span>
    </button>`;

(function() {
    function onDocumentReady() {
        const queryParams = new URLSearchParams(location.search);
        if (queryParams.has('search_query')) {
            const searchBar = new Searchbar({ params: { query: queryParams.get('search_query') } });
        } else {
            const searchBar = new Searchbar({});
        }
    }

    if (document.readyState !== 'loading') {
        onDocumentReady();
    } else {
        document.addEventListener('DOMContentLoaded', onDocumentReady);
    }
})();

export default Searchbar;
