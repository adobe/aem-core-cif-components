/*******************************************************************************
 *
 *     Copyright ${year} Adobe. All rights reserved.
 *     This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License. You may obtain a copy
 *     of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under
 *     the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *     OF ANY KIND, either express or implied. See the License for the specific language
 *     governing permissions and limitations under the License.
 *
 ******************************************************************************/

'use strict';

const minicartEdit = `<form class="cartOptions__root">
    <div class="cartOptions__focusItem">
        <span class="cartOptions__name">{{name}} {{currency}}{{price}}</span>
    </div>
    <div class="cartOptions__form">
        <section class="cartOptions__quantity">
            <h2 class="cartOptions__quantityTitle option__title">Quantity</h2>
            <div class="quantity__root">
                <span class="fieldIcons__root" style="--iconsBefore:0; --iconsAfter:1;">
                    <span class="fieldIcons__input">
                        <select aria-label="product's quantity" class="select__input field__input" name="quantity">
                            <option value="1">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                            <option value="4">4</option>
                        </select>
                    </span><span class="fieldIcons__before"></span>
                        <span class="fieldIcons__after"><span class="icon__root">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
                        </span>
                    </span>
                </span>
                <p class="message__root"></p></div>
        </section>
    </div>
    <div class="cartOptions__save">
        <button class="button-root_normalPriority button__root clickable__root" type="button" data-action="cancel">
            <span class="button__content">Cancel</span>
        </button>
        <button class="button__root_highPriority button__root clickable__root button__filled" type="button" data-action="save">
            <span class="button__content">Update Cart</span>
        </button>
    </div>
    <div class="cartOptions__modal">
        <div class="indicator__root">
            <img class="indicator__indicator" src="https://magento-venia-concept-obzom.local.pwadev:8776/8244c0fdc106dd57437fa5a48d27ab0d.svg" width="64" height="64" alt="Loading indicator">
            <span class="indicator__message">Updating Cart</span>
        </div>
    </div>
</form>`;

window.CIF = window.CIF || {};
window.CIF.MiniCart.templates = window.CIF.MiniCart.templates || {};
window.CIF.MiniCart.templates.edit = minicartEdit;