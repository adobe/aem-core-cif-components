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

import { stripIgnoredCharacters } from 'graphql';

/**
 * This custom fetch interceptor minimizes GraphQL queries, by stripping unnecessary characters from the query.
 * This is a workaround, since Apollo always restores the formatting of a query, independent of the formatting in
 * .graphql files. See https://github.com/apollographql/apollo-link/issues/1079.
 *
 * This is required, since there are certain limits for Magento Cloud, see
 * https://docs.fastly.com/en/guides/resource-limits#request-and-response-limits.
 */
export default function(url, options) {
    try {
        if (options.method === 'GET') {
            // Split url by ? to get query string
            let parts = url.split('?');

            // Only proceed if it is a valid URL
            if (parts.length === 2) {
                // Parse query from search params string
                let params = new URLSearchParams(parts[1]);
                let query = params.get('query');
                if (query) {
                    query = stripIgnoredCharacters(query);
                    params.set('query', query);
                    parts[1] = params.toString();
                    url = parts.join('?');
                }
            }
        }
        if (options.method === 'POST') {
            // Parse query from body in request
            let bodyJson = JSON.parse(options.body);
            if (bodyJson.query) {
                bodyJson.query = stripIgnoredCharacters(bodyJson.query);
                options.body = JSON.stringify(bodyJson);
            }
        }
    } catch (err) {
        console.warn('Could not minimize GraphQL query', err);
    }
    return fetch(url, options);
}
