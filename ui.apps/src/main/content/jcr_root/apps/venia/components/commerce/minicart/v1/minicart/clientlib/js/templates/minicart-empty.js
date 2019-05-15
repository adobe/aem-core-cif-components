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

const emptyMiniCart = `<div class="emptyMiniCart__root">
<h3 class="emptyMiniCart__emptyTitle">There are no items in your shopping cart</h3>
<button class="trigger__root clickable__root">
<span class="emptyMiniCart__continue button__root clickable__root">Continue Shopping</span></button></div>`;

window.CIF = window.CIF || {};
window.CIF.MiniCart.templates = window.CIF.MiniCart.templates || {};
window.CIF.MiniCart.templates.emptyMiniCart = emptyMiniCart;