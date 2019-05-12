/*******************************************************************************
 * ADOBE CONFIDENTIAL
 * __________________
 *
 * Copyright 2019 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 ******************************************************************************/

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
