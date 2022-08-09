/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
import { default as addToCartHandler } from './addToCart';
import { default as categoryPageViewHandler } from './categoryPageView';
import { default as completeCheckoutHandler } from './completeCheckout';
import { default as createAccountHandler } from './createAccount';
import { default as editAccountHandler } from './editAccount';
import { default as pageViewHandler } from './pageView';
import { default as placeOrderHandler } from './placeOrder';
import { default as productPageViewHandler } from './productPageView';
import { default as searchRequestSentHandler } from './searchRequestSent';
import { default as searchResponseReceivedHandler } from './searchResponseReceived';
import { default as shoppingCartPageViewHandler } from './shoppingCartPageView';
import { default as shoppingMiniCartViewHandler } from './shoppingMiniCartView';
import { default as startCheckoutHandler } from './startCheckout';
import { default as signInHandler } from './signIn';

export default [
    addToCartHandler,
    categoryPageViewHandler,
    completeCheckoutHandler,
    createAccountHandler,
    editAccountHandler,
    pageViewHandler,
    placeOrderHandler,
    productPageViewHandler,
    searchRequestSentHandler,
    searchResponseReceivedHandler,
    shoppingCartPageViewHandler,
    shoppingMiniCartViewHandler,
    startCheckoutHandler,
    signInHandler
];
