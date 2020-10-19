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

    const CLEAR_BUTTON_HTML = `<button class="trigger__root clickable__root" type="button">
            <span class="icon__root">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </span>
            </button>`;

    const SEARCHBAR_TOGGLE = '.searchTrigger__root';

    class Searchbar {
        constructor(props) {
            this._classes = {
                open: 'searchBar__root_open'
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
                const html = new DOMParser().parseFromString(CLEAR_BUTTON_HTML, 'text/html');
                this._resetButtonElement = html.body.firstChild;
            }
            return this._resetButtonElement;
        }

        _installListeners() {
            let that = this;
            // listen to onclick on the "search" icon in the header
            document.querySelector(SEARCHBAR_TOGGLE).addEventListener('click', event => {
                this.toggle();
            });
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
            const afterField = this._searchBarRoot.querySelector('.fieldIcons__after');
            afterField.appendChild(this._getOrCreateResetButton());

            this._searchBarRoot.querySelector('.fieldIcons__after .trigger__root').addEventListener('click', e => {
                //clear the search field
                this._searchBox.value = '';

                //remove the reset button
                afterField.removeChild(afterField.childNodes[0]);

                //re-register the listener on the searchbox
                this._registerSearchBoxListener();
            });
        }

        _registerSearchBoxListener() {
            const _handleKeyDown = e => {
                const input = e.currentTarget;
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
        searchBox: "input[role='searchbox']"
    };

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
