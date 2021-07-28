/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
export default {
    products: {
        items: [
            {
                sku: 'VA24',
                __typename: 'BundleProduct',
                id: 2519,
                name: 'Night Out Collection',
                dynamic_sku: true,
                dynamic_price: true,
                dynamic_weight: true,
                price_view: 'PRICE_RANGE',
                ship_bundle_items: 'TOGETHER',
                items: [
                    {
                        option_id: 1,
                        title: 'Necklace',
                        required: true,
                        type: 'checkbox',
                        position: 1,
                        sku: 'VA24',
                        options: [
                            {
                                id: 1,
                                quantity: 1,
                                position: 1,
                                is_default: true,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Carmina Necklace',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 78,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            },
                            {
                                id: 2,
                                quantity: 1,
                                position: 2,
                                is_default: false,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Augusta Necklace',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 98,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            }
                        ],
                        __typename: 'BundleItem'
                    },
                    {
                        option_id: 2,
                        title: 'Cirque Earrings',
                        required: true,
                        type: 'select',
                        position: 2,
                        sku: 'VA24',
                        options: [
                            {
                                id: 3,
                                quantity: 1,
                                position: 1,
                                is_default: true,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: true,
                                label: 'Gold Cirque Earrings',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 68,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            },
                            {
                                id: 4,
                                quantity: 1,
                                position: 2,
                                is_default: false,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Silver Cirque Earrings',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 68,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            }
                        ],
                        __typename: 'BundleItem'
                    },
                    {
                        option_id: 3,
                        title: 'Sol Earrings',
                        required: true,
                        type: 'radio',
                        position: 3,
                        sku: 'VA24',
                        options: [
                            {
                                id: 5,
                                quantity: 1,
                                position: 1,
                                is_default: true,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Gold Sol Earrings',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 48,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            },
                            {
                                id: 6,
                                quantity: 1,
                                position: 2,
                                is_default: false,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: true,
                                label: 'Silver Sol Earrings',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 48,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            }
                        ],
                        __typename: 'BundleItem'
                    },
                    {
                        option_id: 4,
                        title: 'Bangles',
                        required: true,
                        type: 'multi',
                        position: 4,
                        sku: 'VA24',
                        options: [
                            {
                                id: 7,
                                quantity: 1,
                                position: 1,
                                is_default: true,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Gold Omni Bangle Set',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 78,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            },
                            {
                                id: 8,
                                quantity: 1,
                                position: 2,
                                is_default: true,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Semper Bangle Set',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 108,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            },
                            {
                                id: 9,
                                quantity: 1,
                                position: 3,
                                is_default: false,
                                price: 0,
                                price_type: 'FIXED',
                                can_change_quantity: false,
                                label: 'Silver Amor Bangle Set',
                                product: {
                                    price_range: {
                                        maximum_price: {
                                            final_price: {
                                                currency: 'USD',
                                                value: 98,
                                                __typename: 'Money'
                                            },
                                            __typename: 'ProductPrice'
                                        },
                                        __typename: 'PriceRange'
                                    },
                                    __typename: 'SimpleProduct'
                                },
                                __typename: 'BundleItemOption'
                            }
                        ],
                        __typename: 'BundleItem'
                    }
                ]
            }
        ]
    }
};
