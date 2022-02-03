/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import { useEffect, useCallback } from 'react';
import { useQuery, useApolloClient } from '@apollo/client';
import { checkCookie, cookieValue } from './cookieUtils';
import { isDataLayerEnabled } from './dataLayerUtils';

import QUERY_COUNTRIES from '../queries/query_countries.graphql';

export const useEventListener = (target, type, listener, ...rest) => {
    useEffect(() => {
        target.addEventListener(type, listener, ...rest);

        // return a callback, which is called on unmount
        return () => {
            target.removeEventListener(type, listener, ...rest);
        };
    }, [listener, rest, target, type]);
};

export const useCookieValue = cookieName => {
    if (!cookieName || cookieName.length === 0) {
        return '';
    }
    let value = checkCookie(cookieName) ? cookieValue(cookieName) : '';
    const setCookieValue = (value, age) => {
        const cookieSettings = `path=/; domain=${window.location.hostname};Max-Age=${age !== undefined ? age : 3600}`;
        document.cookie = `${cookieName}=${value};${cookieSettings}`;
    };

    return [value, setCookieValue];
};

export const useCountries = () => {
    const { data, error } = useQuery(QUERY_COUNTRIES);
    if (error || !data || !data.countries) {
        return { error, countries: [] };
    }

    return { countries: data.countries };
};

/**
 * This hook is taken from the Peregrine library.
 * We don't use it because upgrading to the peregrine library that exports it would mean bringing in some dependencies we don't need (i.e. Redux)
 *
 * @param {DocumentNode} query - parsed GraphQL operation description
 *
 * @returns {Function} callback that runs the query and returns a Promise
 */
export const useAwaitQuery = query => {
    const apolloClient = useApolloClient();

    return useCallback(
        options => {
            return apolloClient.query({
                ...options,
                query
            });
        },
        [apolloClient, query]
    );
};

/**
 * This hook makes the query parameters of the URL available.
 */
export const useQueryParams = () => {
    // Better to use useLocation from react router here, but this doesn't work because of dependency mess up.
    return new URLSearchParams(window.location.search);
};

/**
 * This hook provides access to the Magento Storefront Events SDK if enabled and available.
 */
export const useStorefrontEvents = () => {
    if (isDataLayerEnabled && window.magentoStorefrontEvents) {
        return window.magentoStorefrontEvents;
    }
    return false;
};

export const usePageType = () => {
    const PageTypes = {
        // Pages with landing-page template or pages that match the store root URL
        // as defined in the store config.
        CMS: 'CMS',

        // Pages with category component or category-page template
        CATEGORY: 'Category',

        // Pages with product component or product-page template
        PRODUCT: 'Product',

        // Pages with cart component
        CART: 'Cart',

        // Pages with checkout component
        CHECKOUT: 'Checkout',

        // Any other pages
        PAGE_BUILDER: 'PageBuilder'
    };

    // Detect homepage, either by template name or by URL
    let template = document.querySelector('meta[name="template"]');
    if (template) {
        template = template.getAttribute('content');
    }

    let canonicalUrl = document.querySelector('link[rel="canonical"]');
    if (canonicalUrl) {
        canonicalUrl = canonicalUrl.getAttribute('href');
    }

    let storeRootUrl = null;
    try {
        let storeConfig = JSON.parse(document.querySelector('meta[name="store-config"]').getAttribute('content'));
        storeRootUrl = storeConfig.storeRootUrl;
    } catch (err) {
        // Could not parse store config, ignore for now
    }

    if (template == 'landing-page' || (canonicalUrl && storeRootUrl && canonicalUrl.endsWith(storeRootUrl))) {
        return PageTypes.CMS;
    }
    if (document.querySelector('[data-cif-product-context]') || template == 'product-page') {
        return PageTypes.PRODUCT;
    }
    if (document.querySelector('[data-cif-category-context]') || template == 'category-page') {
        return PageTypes.CATEGORY;
    }
    if (document.querySelector('.cartcontainer__root')) {
        return PageTypes.CART;
    }
    if (document.querySelector('.checkoutpage__root')) {
        return PageTypes.CHECKOUT;
    }

    return PageTypes.PAGE_BUILDER;
};
