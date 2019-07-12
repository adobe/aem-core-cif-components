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
    let fetchGraphqlSpy;

    beforeEach(() => {
        fetchSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetch');
        graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql' });
    });

    afterEach(() => {
        fetchSpy.restore();
        fetchGraphqlSpy && fetchGraphqlSpy.restore();
    });

    it('throws an error if endpoint is missing', () => {
        assert.throws(() => new CommerceGraphqlApi());
    });

    it('fetches an uncached GraphQL query', () => {
        const mockResult = { result: 'my-result' };
        fetchSpy.resolves(mockResult);

        let query = 'my-sample-query';

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
        const mockResult = { result: 'my-result' };
        fetchSpy.resolves(mockResult);

        let query = 'my-sample-query';

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
        const mockResult = { errors: [{ message: 'my-error' }] };
        fetchSpy.resolves(mockResult);

        return graphqlApi._fetchGraphql('sample-query', false).then(
            () => Promise.reject(new Error('Expected promise rejection')),
            err => {
                assert.equal(err.message, JSON.stringify(mockResult.errors));
            }
        );
    });

    it('retrieves product prices without variants', () => {
        const mockResponse = {
            data: {
                products: {
                    items: [
                        {
                            sku: 'sku-a',
                            price: {
                                regularPrice: {
                                    amount: {
                                        currency: 'USD',
                                        value: 118
                                    }
                                }
                            }
                        },
                        {
                            sku: 'sku-b',
                            price: {
                                regularPrice: {
                                    amount: {
                                        currency: 'USD',
                                        value: 78
                                    }
                                }
                            }
                        }
                    ]
                }
            }
        };

        fetchGraphqlSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetchGraphql').resolves(mockResponse);
        graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql' });

        return graphqlApi.getProductPrices(['sku-a', 'sku-b'], false).then(res => {
            assert.isTrue(fetchGraphqlSpy.calledOnce);

            // Make sure that query does not contain "variants"
            let query = fetchGraphqlSpy.firstCall.args[0];
            assert.notInclude(query, 'variants');

            // Validate price dictionary
            assert.hasAllKeys(res, ['sku-a', 'sku-b']);
            assert.equal(res['sku-a'].currency, 'USD');
            assert.equal(res['sku-a'].value, 118);
        });
    });

    it('retrieves product prices with variants', () => {
        const mockResponse = {
            data: {
                products: {
                    items: [
                        {
                            sku: 'sku-a',
                            price: {
                                regularPrice: {
                                    amount: {
                                        currency: 'USD',
                                        value: 118
                                    }
                                }
                            },
                            variants: [
                                {
                                    product: {
                                        sku: 'sku-a-xl',
                                        price: {
                                            regularPrice: {
                                                amount: {
                                                    currency: 'USD',
                                                    value: 200
                                                }
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    ]
                }
            }
        };

        fetchGraphqlSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetchGraphql').resolves(mockResponse);
        graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql' });

        return graphqlApi.getProductPrices(['sku-a'], true).then(res => {
            assert.isTrue(fetchGraphqlSpy.calledOnce);

            // Make sure that query contains "variants"
            let query = fetchGraphqlSpy.firstCall.args[0];
            assert.include(query, 'variants');

            // Validate price dictionary
            assert.hasAllKeys(res, ['sku-a', 'sku-a-xl']);
            assert.equal(res['sku-a'].currency, 'USD');
            assert.equal(res['sku-a'].value, 118);
            assert.equal(res['sku-a-xl'].currency, 'USD');
            assert.equal(res['sku-a-xl'].value, 200);
        });
    });

    it('retrieves product image urls', () => {
        const mockResponse = {
            data: {
                products: {
                    items: [
                        {
                            sku: 'sku-a',
                            name: 'product-a',
                            thumbnail: {
                                url: '/vsk12-la_main_3.jpg'
                            },
                            variants: [
                                {
                                    product: {
                                        sku: 'sku-a-xl',
                                        thumbnail: {
                                            url: '/vsk12-la_main_2.jpg'
                                        }
                                    }
                                }
                            ]
                        },
                        {
                            sku: 'sku-b',
                            name: 'product-b',
                            thumbnail: {
                                url: '/vsk02-ll_main_2.jpg'
                            }
                        }
                    ]
                }
            }
        };

        fetchGraphqlSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetchGraphql').resolves(mockResponse);
        graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql' });

        return graphqlApi.getProductImageUrls({ 'product-a': 'sku-a-xl', 'product-b': 'sku-b' }).then(res => {
            assert.isTrue(fetchGraphqlSpy.calledOnce);

            // Validate image url dictionary
            assert.hasAllKeys(res, ['sku-a-xl', 'sku-b']);
            assert.equal(res['sku-a-xl'], '/vsk12-la_main_2.jpg');
            assert.equal(res['sku-b'], '/vsk02-ll_main_2.jpg');
        });
    });
});
