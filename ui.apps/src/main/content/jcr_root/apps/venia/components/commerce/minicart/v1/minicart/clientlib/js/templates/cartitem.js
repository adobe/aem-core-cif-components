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

window.CIF.MiniCart = window.CIF.MiniCart || {};

const cartItem = `

    <div class="product__image"
         style="min-height: 100px; width: 80px; background-image: url('/img/resize/80?url=%2Fmedia%2Fcatalog%2Fproduct%2Fv%2Fa%2Fva12-ts_main.jpg');"></div>
    <div class="product__name">{{name}}</div>
    <div class="product__quantity">
        <div class="product__quantityRow">
            <span>{{qty}}</span>
            <span class="product__quantityOperator">×</span>
            <span class="product__price">{{currency}}{{price}}</span>
        </div>
    </div>
    <div class="kebab__root">
        <button class="kebab__kebab"><span class="icon__root icon__root-kebab"></span></button>
        <ul class="kebab__dropdown">
            <li class="section__menuItem">
                <button data-action="favorite"><span class="icon__root icon__root_heart"></span><span class="section__text">Add to favorites</span></button>
            </li>
            <li class="section__menuItem"><button data-action="edit"><span class="icon__root icon__root_pencil"></span><span class="section__text">Edit item</span></button>
            </li>
            <li class="section__menuItem">
                <button data-action="remove"><span class="icon__root icon__root_trash"></span><span class="section__text">Remove item</span></button>
            </li>
        </ul>
    </div>
 
`;


window.CIF.MiniCart.templates = window.CIF.MiniCart.templates || {};
window.CIF.MiniCart.templates.cartItem = cartItem;
