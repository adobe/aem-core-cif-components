/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
export const searchResponseEvent = {
    type: 'SEARCH_RESPONSE',
    payload: {
        categories: [
            {
                __typename: 'AggregationOption',
                label: 'Bottoms',
                value: '11'
            },
            {
                __typename: 'AggregationOption',
                label: 'Pants & Shorts',
                value: '12'
            }
        ],
        facets: [],
        page: 1,
        perPage: 3,
        products: [
            {
                __typename: 'ConfigurableProduct',
                id: 1144,
                uid: 'MTE0NA==',
                sku: 'VP01',
                name: 'Selena Pants',
                small_image: {
                    __typename: 'ProductImage',
                    url:
                        'https://beacon-rjroszy-vzsrtettsztvg.us-4.magentosite.cloud/media/catalog/product/cache/37f3b100da589f62b6681aad6ae5936f/v/p/vp01-ll_main_2.jpg'
                },
                url_key: 'selena-pants',
                url_suffix: '.html',
                price: {
                    __typename: 'ProductPrices',
                    regularPrice: {
                        __typename: 'Price',
                        amount: {
                            __typename: 'Money',
                            value: 108,
                            currency: 'USD'
                        }
                    }
                },
                price_range: {
                    __typename: 'PriceRange',
                    maximum_price: {
                        __typename: 'ProductPrice',
                        final_price: {
                            __typename: 'Money',
                            currency: 'USD',
                            value: 108
                        },
                        discount: {
                            __typename: 'ProductDiscount',
                            amount_off: 0
                        }
                    }
                }
            }
        ],
        searchRequestId: 'selena',
        searchUnitId: 'search-bar',
        suggestions: [
            {
                __typename: 'ConfigurableProduct',
                id: 1144,
                uid: 'MTE0NA==',
                sku: 'VP01',
                name: 'Selena Pants',
                small_image: {
                    __typename: 'ProductImage',
                    url:
                        'https://beacon-rjroszy-vzsrtettsztvg.us-4.magentosite.cloud/media/catalog/product/cache/37f3b100da589f62b6681aad6ae5936f/v/p/vp01-ll_main_2.jpg'
                },
                url_key: 'selena-pants',
                url_suffix: '.html',
                price: {
                    __typename: 'ProductPrices',
                    regularPrice: {
                        __typename: 'Price',
                        amount: {
                            __typename: 'Money',
                            value: 108,
                            currency: 'USD'
                        }
                    }
                },
                price_range: {
                    __typename: 'PriceRange',
                    maximum_price: {
                        __typename: 'ProductPrice',
                        final_price: {
                            __typename: 'Money',
                            currency: 'USD',
                            value: 108
                        },
                        discount: {
                            __typename: 'ProductDiscount',
                            amount_off: 0
                        }
                    }
                }
            }
        ]
    }
};
