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

// This is used to validate the queries against the Magento GraphQL schema
import { buildClientSchema, parse, validate } from 'graphql';
import magentoSchema234 from './magento-schema-2.3.4.json';
import magentoSchema235 from './magento-schema-2.3.5.json';
let magentoSchemas = {
    '2.3.4': magentoSchema234,
    '2.3.5': magentoSchema235
};

describe('CommerceGraphqlApi', () => {
    let graphqlApi;
    let fetchSpy;
    let fetchGraphqlSpy;

    beforeEach(() => {
        fetchSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetch');
        graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql', storeView: 'default' });
    });

    afterEach(() => {
        fetchSpy.restore();
        fetchGraphqlSpy && fetchGraphqlSpy.restore();
    });

    it('throws an error if a required property is missing', () => {
        assert.throws(() => new CommerceGraphqlApi());
    });

    it('throws an error if the endpoint property is missing', () => {
        assert.throws(() => new CommerceGraphqlApi({ storeView: 'default' }));
    });

    it('throws an error if the storeView property is missing', () => {
        assert.throws(() => new CommerceGraphqlApi({ endpoint: '/graphql' }));
    });

    it('fetches an uncached GraphQL query', () => {
        const mockResult = { result: 'my-result' };
        fetchSpy.resolves(mockResult);

        let query = 'my-sample-query';

        return graphqlApi._fetchGraphql(query, true).then(res => {
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

    it('passes the storeView header', () => {
        const mockResult = { result: 'my-result' };
        fetchSpy.resolves(mockResult);
        graphqlApi.storeView = 'my-special-store';

        let query = 'my-sample-query';

        return graphqlApi._fetchGraphql(query, true).then(res => {
            assert.isTrue(fetchSpy.calledOnce);
            let options = fetchSpy.firstCall.args[1];

            assert.include(options.headers, { Store: 'my-special-store' });
        });
    });

    it('fetches a cached GraphQL query', () => {
        const mockResult = { result: 'my-result' };
        fetchSpy.resolves(mockResult);

        let query = 'my-sample-query';

        return graphqlApi._fetchGraphql(query).then(res => {
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

    for (const [version, magentoSchema] of Object.entries(magentoSchemas)) {
        it('retrieves product prices without variants with Magento schema ' + version, () => {
            const mockResponse = {
                data: {
                    products: {
                        items: [
                            {
                                sku: 'sku-a',
                                price_range: {
                                    minimum_price: {
                                        regular_price: {
                                            value: 118,
                                            currency: 'USD'
                                        },
                                        final_price: {
                                            value: 118,
                                            currency: 'USD'
                                        },
                                        discount: {
                                            amount_off: 0,
                                            percent_off: 0
                                        }
                                    }
                                }
                            },
                            {
                                sku: 'sku-b',
                                price_range: {
                                    minimum_price: {
                                        regular_price: {
                                            value: 78,
                                            currency: 'USD'
                                        },
                                        final_price: {
                                            value: 78,
                                            currency: 'USD'
                                        },
                                        discount: {
                                            amount_off: 0,
                                            percent_off: 0
                                        }
                                    }
                                }
                            }
                        ]
                    }
                }
            };

            fetchGraphqlSpy = sinon.stub(CommerceGraphqlApi.prototype, '_fetchGraphql').resolves(mockResponse);
            graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql', storeView: 'default' });

            return graphqlApi.getProductPrices(['sku-a', 'sku-b'], false).then(res => {
                assert.isTrue(fetchGraphqlSpy.calledOnce);

                // Make sure that query does not contain "variants"
                let query = fetchGraphqlSpy.firstCall.args[0];
                assert.notInclude(query, 'variants');

                // Validate the query against Magento GraphQL schema
                let schema = buildClientSchema(magentoSchema.data);
                let errors = validate(schema, parse(query));
                assert.isTrue(errors.length == 0);

                // Validate price dictionary
                assert.hasAllKeys(res, ['sku-a', 'sku-b']);
                assert.equal(res['sku-a'].minimum_price.final_price.currency, 'USD');
                assert.equal(res['sku-a'].minimum_price.final_price.value, 118);
            });
        });
    }

    for (const [version, magentoSchema] of Object.entries(magentoSchemas)) {
        it('retrieves product prices with variants with Magento schema ' + version, () => {
            const mockResponse = {
                data: {
                    products: {
                        items: [
                            {
                                sku: 'sku-a',
                                price_range: {
                                    minimum_price: {
                                        regular_price: {
                                            value: 118,
                                            currency: 'USD'
                                        },
                                        final_price: {
                                            value: 118,
                                            currency: 'USD'
                                        },
                                        discount: {
                                            amount_off: 0,
                                            percent_off: 0
                                        }
                                    }
                                },
                                variants: [
                                    {
                                        product: {
                                            sku: 'sku-a-xl',
                                            price_range: {
                                                minimum_price: {
                                                    regular_price: {
                                                        value: 200,
                                                        currency: 'USD'
                                                    },
                                                    final_price: {
                                                        value: 200,
                                                        currency: 'USD'
                                                    },
                                                    discount: {
                                                        amount_off: 0,
                                                        percent_off: 0
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
            graphqlApi = new CommerceGraphqlApi({ endpoint: '/graphql', storeView: 'default' });

            return graphqlApi.getProductPrices(['sku-a'], true).then(res => {
                assert.isTrue(fetchGraphqlSpy.calledOnce);

                // Make sure that query contains "variants"
                let query = fetchGraphqlSpy.firstCall.args[0];
                assert.include(query, 'variants');

                // Validate the query against Magento GraphQL schema
                let schema = buildClientSchema(magentoSchema.data);
                let errors = validate(schema, parse(query));
                assert.isTrue(errors.length == 0);

                // Validate price dictionary
                assert.hasAllKeys(res, ['sku-a', 'sku-a-xl']);
                assert.equal(res['sku-a'].minimum_price.final_price.currency, 'USD');
                assert.equal(res['sku-a'].minimum_price.final_price.value, 118);
                assert.equal(res['sku-a-xl'].minimum_price.final_price.currency, 'USD');
                assert.equal(res['sku-a-xl'].minimum_price.final_price.value, 200);
            });
        });
    }
});
