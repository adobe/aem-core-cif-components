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

import CommerceGraphqlApi from '../../../src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js';

describe('CommerceGraphqlApi', () => {

    let graphqlApi;
    let fetchSpy;

    beforeEach(() => {
        fetchSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetch');
        graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql' });
    });

    afterEach(() => {
        fetchSpy.restore();
    })

    it('fetches an uncached GraphQL query', () => {
        const mockResult = { result: 'my-result'};
        fetchSpy.resolves(mockResult);

        let query = 'abc';

        return graphqlApi._fetchGraphql(query, false).then(res => {
            // Verify requests
            assert.isTrue(fetchSpy.calledOnce);
            let fetchArguments = fetchSpy.firstCall.args;

            // Verify request url
            assert.equal(fetchArguments[0], '/graphql');

            // Verify request parameters
            assert.equal(fetchArguments[1].method, 'POST');
            assert.deepEqual(fetchArguments[1].body, JSON.stringify({ query }));

            // Verify result
            assert.deepEqual(res, mockResult);
        });
    });

    it('fetches a cached GraphQL query', () => {
        const mockResult = { result: 'my-result'};
        fetchSpy.resolves(mockResult);

        let query = 'abc';

        return graphqlApi._fetchGraphql(query, true).then(res => {
            // Verify requests
            assert.isTrue(fetchSpy.calledOnce);
            let fetchArguments = fetchSpy.firstCall.args;

            // Verify request url
            assert.include(fetchArguments[0], '/graphql?');

            // verify query parameter
            let queryParams = new URLSearchParams(fetchArguments[0].substr(8));
            assert.isTrue(queryParams.has('query'));
            assert.equal(queryParams.get('query'), query);

            // Verify request parameters
            assert.equal(fetchArguments[1].method, 'GET');

            // Verify result
            assert.deepEqual(res, mockResult);
        });
    });

    it('throws an error for GraphQL errors', () => {

    });

    it('retrieves product prices', () => {

    });

    it('retrieves product image urls', () => {

    });

});