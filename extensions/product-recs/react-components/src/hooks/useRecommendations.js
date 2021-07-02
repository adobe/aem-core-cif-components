/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

import { useEffect, useState } from 'react';

import RecommendationsClient from '@magento/recommendations-js-sdk';
import { useStorefrontEvents, usePageType } from '@adobe/aem-core-cif-react-components';

import { useStorefrontInstanceContext } from '../context/StorefrontInstanceContext';

const commaToOrList = list => list.split(',').join(' OR ');

export const useRecommendations = props => {
    const mse = useStorefrontEvents();
    const { context: storefrontInstance, error: storefrontInstanceError } = useStorefrontInstanceContext();
    const [data, setData] = useState({ loading: true, data: null });
    const pageType = usePageType();

    const {
        title,
        recommendationType,
        categoryExclusions,
        categoryInclusions,
        excludeMaxPrice,
        excludeMinPrice,
        includeMaxPrice,
        includeMinPrice
    } = props;

    useEffect(() => {
        // Stop loading if there is an error
        if (storefrontInstanceError) {
            setData({ loading: false, data: null });
            return;
        }

        // Skip if storefront instance context is not yet set
        if (!storefrontInstance) {
            return;
        }

        (async () => {
            // If no parameters are passed, everything is automatically taken from MSE
            const client = new RecommendationsClient({ alternateEnvironmentId: '', pageType });

            // Add filtering
            // This is currently limited to a single filter by the recommendations SDK
            let filter;
            if (categoryExclusions) {
                filter = `-categories: (${commaToOrList(categoryExclusions)})`;
            }
            if (includeMinPrice) {
                filter = `prices.minimum.final: >${includeMinPrice}`;
            }
            if (includeMaxPrice) {
                filter = `prices.maximum.final: <${includeMaxPrice}`;
            }
            if (excludeMinPrice) {
                filter = `prices.minimum.final: <${excludeMinPrice}`;
            }
            if (excludeMaxPrice) {
                filter = `prices.minimum.final: >${excludeMaxPrice}`;
            }
            if (categoryInclusions) {
                filter = `categories: (${commaToOrList(categoryInclusions)})`;
            }

            client.register({ name: title, type: recommendationType, filter });

            mse && mse.publish.recsRequestSent();
            const { status, data } = await client.fetch();
            if (status !== 200 || !data || !data.units || data.units.length === 0) {
                console.warn('Could not load product recommendations', status);
                setData({ loading: false, data: null });
                return;
            }

            if (mse) {
                const { units = [] } = mse.context.getRecommendations() || {};
                mse.context.setRecommendations({ units: [...units, ...data.units] });
                mse.publish.recsResponseReceived();
            }

            setData({ loading: false, data });
        })();
    }, [storefrontInstance, storefrontInstanceError]);

    return data;
};
