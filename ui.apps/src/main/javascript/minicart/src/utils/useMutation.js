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
import { useCallback, useMemo } from 'react';

import { useApolloContext, useQueryResult } from '@magento/peregrine';

/**
 * A [React Hook]{@link https://reactjs.org/docs/hooks-intro.html} that provides
 * access to query results data and an API object for running the query and
 * managing a query result state object.
 *
 * @kind function
 *
 * @param {DocumentNode} query A GraphQL document containing a query to send to the server. See {@link https://github.com/apollographql/graphql-tag graphql-tag}
 *
 * @return {Object[]} An array with two entries containing the following content: [{@link ../useQueryResult#queryresultstate--object QueryResultState}, {@link API}]
 */
export const useMutation = (mutation, toRefetch) => {
    const apolloClient = useApolloContext();
    const [queryResultState, queryResultApi] = useQueryResult();
    const { receiveResponse } = queryResultApi;

    /**
     * A callback function that performs a query either as an effect or in response to user interaction.
     *
     * @function API.runQuery
     *
     * @param {DocumentNode} query A GraphQL document
     */
    const runQuery = useCallback(
        async ({ variables }) => {
            let payload;
            try {
                console.log(`Do we have variables? `, variables);
                payload = await apolloClient.mutate({ mutation, variables, refetchQueries: toRefetch });
                console.log(`Payload of the mutation is `, payload);
            } catch (e) {
                console.log(`Stuff happended: `, e);
                payload = {
                    error: e
                };
            }
            receiveResponse(payload);
        },
        [apolloClient, mutation, receiveResponse]
    );

    /**
     * The API for managing the query.
     * Use this API to run queries and get the resulting state values and query data.
     *
     * In addition to the {@link API.runQuery runQuery()} function,
     * this object also contains the API methods from the {@link ../useQueryResult#api--object  useQueryResult hook}.
     *
     * @typedef API
     * @type Object
     */
    const api = useMemo(
        () => ({
            ...queryResultApi,
            runQuery
        }),
        [queryResultApi, runQuery]
    );

    return [queryResultState, api];
};
