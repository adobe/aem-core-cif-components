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
import { useEffect, useState } from 'react';

import RecommendationsClient from '@magento/recommendations-js-sdk';
import { usePageType } from '@adobe/aem-core-cif-react-components';

import { useStorefrontInstanceContext } from '../context/StorefrontInstanceContext';

const commaToOrList = list => list.split(',').join(' OR ');

export const useRecommendations = props => {
    const { context: storefrontInstance, error: storefrontInstanceError, mse } = useStorefrontInstanceContext();
    const [data, setData] = useState({ loading: true, units: null });
    const pageType = usePageType();

    const {
        title,
        recommendationType,
        categoryExclusions,
        categoryInclusions,
        excludeMaxPrice,
        excludeMinPrice,
        includeMaxPrice,
        includeMinPrice,
        preconfigured = false
    } = props;

    const getFilters = () => {
        let result = [];

        // This is currently limited to a single filter by the recommendations SDK
        if (categoryInclusions) {
            result.push(`categories: (${commaToOrList(categoryInclusions)})`);
        }
        if (categoryExclusions) {
            result.push(`-categories: (${commaToOrList(categoryExclusions)})`);
        }
        if (includeMinPrice) {
            result.push(`prices.minimum.final: >${includeMinPrice}`);
        }
        if (includeMaxPrice) {
            result.push(`prices.maximum.final: <${includeMaxPrice}`);
        }
        if (excludeMinPrice) {
            result.push(`prices.maximum.final: <${excludeMinPrice}`);
        }
        if (excludeMaxPrice) {
            result.push(`prices.minimum.final: >${excludeMaxPrice}`);
        }
        return result;
    };

    const translateFilters = (filters, storeViewCode) => {
        // Translate all filters, except for the first one. The first one is translated by the Recommendations SDK.
        if (filters.length < 2) {
            return filters;
        }

        for (let i = 1; i < filters.length; i++) {
            const split = filters[i].split(':');
            split[0] = split[0]
                .replace('categories', `product.${storeViewCode}.categories`)
                .replace('prices', `product.${storeViewCode}.prices`);

            filters[i] = split.join(':');
        }

        return filters;
    };

    useEffect(() => {
        // Stop loading if there is an error while retrieving the storefront instance context
        if (storefrontInstanceError) {
            setData({ loading: false, units: null });
            return;
        }

        // Skip if storefront instance context is not yet available
        if (!storefrontInstance) {
            return;
        }

        (async () => {
            // If no parameters are passed, everything is automatically taken from MSE
            const client = new RecommendationsClient({ alternateEnvironmentId: '', pageType });

            if (!preconfigured) {
                // Get all configured filters, translate them, except for the first one and concatenate them with AND.
                // This is considered a workaround and should be fixed in the Recommendations SDK.
                let filters = getFilters();
                filters = translateFilters(filters, client._storeViewCode);
                filters = filters.join(' AND ');

                // Register recommendation as configured in component dialog
                client.register({ name: title, type: recommendationType, filter: filters });
            }

            mse && mse.publish.recsRequestSent();
            const { status, data } = await (preconfigured ? client.fetchPreconfigured() : client.fetch());

            if (
                status !== 200 ||
                !data ||
                ((!data.units || data.units.length === 0) && (!data.results || data.results.length === 0))
            ) {
                setData({ loading: false, units: null });
                return;
            }

            // Fix inconsistency in result API
            const newUnits = preconfigured ? data.results : data.units;

            if (mse) {
                const { units = [] } = mse.context.getRecommendations() || {};
                mse.context.setRecommendations({ units: [...units, ...newUnits] });
                mse.publish.recsResponseReceived();
            }

            setData({ loading: false, units: newUnits });
        })();
    }, [storefrontInstance, storefrontInstanceError, mse]);

    return data;
};
