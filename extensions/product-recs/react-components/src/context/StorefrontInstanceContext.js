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
import React, { useContext, useState, useEffect } from 'react';

import { useAwaitQuery, useStorefrontEvents } from '@adobe/aem-core-cif-react-components';

import QUERY_STOREFRONT_INSTANCE_CONTEXT from '../queries/query_storefront_instance_context.graphql';

const STORAGE_KEY = 'CIF_STOREFRONT_INSTANCE_CONTEXT';

export const StorefrontInstanceContext = React.createContext();

export const StorefrontInstanceContextProvider = (props = {}) => {
    const getStorefrontInstanceContext = useAwaitQuery(QUERY_STOREFRONT_INSTANCE_CONTEXT);
    const [storefrontContext, setStorefrontContext] = useState({ context: null, error: null });
    // eslint-disable-next-line react-hooks/rules-of-hook
    const mse = typeof props.mse !== 'undefined' ? props.mse : useStorefrontEvents();

    useEffect(() => {
        (async () => {
            // Try to read storefront context from session storage
            let data = sessionStorage.getItem(STORAGE_KEY);
            if (data !== null) {
                try {
                    data = JSON.parse(data);
                } catch (err) {
                    console.warn('Could not parse storefront instance context from session storage', err);
                    sessionStorage.removeItem(STORAGE_KEY);
                    data = null;
                }
            }

            // Get storefront context from GraphQL if not available in session storage
            if (data === null) {
                let error;
                let storefrontInstanceContext;
                try {
                    // We need a try/catch here, since Magento might return an invalid GraphQL response when sending
                    // a product recs query to an instance that does not have product recs extensions installed.
                    storefrontInstanceContext = await getStorefrontInstanceContext();
                    error = storefrontInstanceContext.error;
                } catch (err) {
                    error = err;
                }

                if (error) {
                    console.error('Could not fetch storefront instance context', error);
                    setStorefrontContext({ context: null, error });
                    return;
                }

                data = storefrontInstanceContext.data;
                sessionStorage.setItem(STORAGE_KEY, JSON.stringify(data));
            }

            const {
                environment,
                environment_id,
                website_id,
                website_code,
                website_name,
                store_url,
                store_id,
                store_code,
                store_name,
                store_view_id,
                store_view_code,
                store_view_name,
                catalog_extension_version
            } = data.dataServicesStorefrontInstanceContext;
            const { base_currency_code } = data.storeConfig;

            const context = {
                environmentId: environment_id,
                environment,
                storeUrl: store_url,
                websiteId: website_id,
                websiteCode: website_code,
                storeId: store_id,
                storeCode: store_code,
                storeViewId: store_view_id,
                storeViewCode: store_view_code,
                websiteName: website_name,
                storeName: store_name,
                storeViewName: store_view_name,
                baseCurrencyCode: base_currency_code,
                storeViewCurrencyCode: base_currency_code,
                catalogExtensionVersion: catalog_extension_version
            };

            // Store storefront context in mse
            mse && mse.context.setStorefrontInstance(context);
            setStorefrontContext({ context, error: null });
        })();
    }, [mse]);

    return (
        <StorefrontInstanceContext.Provider value={storefrontContext}>
            {props.children}
        </StorefrontInstanceContext.Provider>
    );
};

export const useStorefrontInstanceContext = () => useContext(StorefrontInstanceContext);
