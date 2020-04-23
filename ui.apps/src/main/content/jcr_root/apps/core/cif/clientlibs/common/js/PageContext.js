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

/**
 * A context object which controls the page behaviour and holds the state of the various components on the page
 * @type {*|{}}
 */

let PageContext = (function(document) {
    'use strict';

    const checkCookie = cookieName => {
        return document.cookie.split(';').filter(item => item.trim().startsWith(`${cookieName}=`)).length > 0;
    };

    const cookieValue = cookieName => {
        let b = document.cookie.match(`(^|[^;]+)\\s*${cookieName}\\s*=\\s*([^;]+)`);
        return b ? b.pop() : '';
    };

    function PageContext() {
        let cartInfo = {};
        // process the cif.cart cookie, containing the cartId and cartQuote
        const cookieName = 'cif.cart';
        if (checkCookie(cookieName)) {
            const cifCartCookie = cookieValue(cookieName);

            if (cifCartCookie.length > 0) {
                let [cartId, cartQuote] = cifCartCookie.split('#');
                cartInfo = { cartId, cartQuote };
            }
        }

        return {
            cartInfo: cartInfo,
            setCartInfoCookie: function({ cartId, cartQuote }) {
                document.cookie = `${cookieName}=${cartId}#${cartQuote};path=/`;
            }
        };
    }

    return PageContext;
})(window.document);

export default PageContext;
