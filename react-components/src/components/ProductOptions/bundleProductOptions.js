/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { instanceOf } from 'prop-types';
import { useTranslation } from 'react-i18next';
import Checkbox from './checkbox';
import Radio from './radio';
import Select from './select';
import MultiSelect from './multiSelect';

import { useAwaitQuery } from '../../utils/hooks';

import Button from '../Button';
import classes from './bundleProductOptions.css';

import BUNDLE_PRODUCT_QUERY from '../../queries/query_bundle_product.graphql';
import Price from '../Price';

const BundleProductOptions = props => {
    const [t] = useTranslation(['product', 'cart']);
    const optionsContainer = props.container.querySelector('#bundle-product-options');
    const bundleProductQuery = useAwaitQuery(BUNDLE_PRODUCT_QUERY);

    const [bundleState, setBundleState] = useState(null);

    const fetchBundleDetails = async sku => {
        const { data, error } = await bundleProductQuery({ variables: { sku }, fetchPolicy: 'network-only' });

        if (error) {
            throw new Error(error);
        }
        let currencyCode;
        const bundleOptions = data.products.items[0];
        const selections = bundleOptions.items.map(item => {
            return {
                option_id: item.option_id,
                customization: item.options
                    .filter(o => o.is_default)
                    .map(o => {
                        const {
                            id,
                            quantity,
                            product: {
                                price_range: {
                                    maximum_price: {
                                        final_price: { value, currency }
                                    }
                                }
                            }
                        } = o;

                        currencyCode = currency;

                        return {
                            id,
                            quantity,
                            price: value
                        };
                    })
            };
        });
        setBundleState({ options: bundleOptions, selections, currencyCode });
    };

    const handleSelectionChange = (option_id, customization) => {
        const { selections } = bundleState;
        const selectionIndex = selections.findIndex(s => s.option_id === option_id);
        selections.splice(selectionIndex, 1, { option_id, customization });

        setBundleState({ ...bundleState, selections });
    };

    const renderItemOptions = item => {
        const { customization } = bundleState.selections.find(s => s.option_id === item.option_id);
        const sortedOptions = item.options
            .sort((a, b) => a.position - b.position)
            .map(option => {
                const {
                    id,
                    quantity,
                    can_change_quantity,
                    label,
                    product: {
                        price_range: {
                            maximum_price: {
                                final_price: { currency, value }
                            }
                        }
                    }
                } = option;

                return {
                    id,
                    quantity,
                    can_change_quantity,
                    label,
                    currency,
                    price: value
                };
            });
        const { option_id, required } = item;
        switch (item.type) {
            case 'checkbox': {
                return (
                    <Checkbox
                        item={{ option_id, required }}
                        sortedOptions={sortedOptions}
                        customization={customization}
                        handleSelectionChange={handleSelectionChange}
                    />
                );
            }
            case 'radio': {
                return (
                    <Radio
                        item={{ option_id, required }}
                        sortedOptions={sortedOptions}
                        customization={customization}
                        handleSelectionChange={handleSelectionChange}
                    />
                );
            }
            case 'select': {
                return (
                    <Select
                        item={{ option_id, required }}
                        sortedOptions={sortedOptions}
                        customization={customization}
                        handleSelectionChange={handleSelectionChange}
                    />
                );
            }
            case 'multi': {
                return (
                    <MultiSelect
                        item={{ option_id, required }}
                        sortedOptions={sortedOptions}
                        customization={customization}
                        handleSelectionChange={handleSelectionChange}
                    />
                );
            }
        }
    };

    const getTotalPrice = () => {
        const { selections, currencyCode } = bundleState;
        const price = selections.reduce((acc, option) => {
            return (
                acc +
                option.customization.reduce((a, c) => {
                    return a + c.price * c.quantity;
                }, 0)
            );
        }, 0);

        return <Price currencyCode={currencyCode} value={price} className={classes.totalPrice} />;
    };

    if (optionsContainer !== null) {
        const { sku } = optionsContainer.dataset;
        return ReactDOM.createPortal(
            <>
                {bundleState === null ? (
                    <section className={'productFullDetail__section ' + classes.customize}>
                        <Button priority="high" onClick={() => fetchBundleDetails(sku)}>
                            <span>{t('product:customize-bundle', 'Customize')}</span>
                        </Button>
                    </section>
                ) : (
                    <>
                        {bundleState.options.items.map(e => (
                            <section key={`item-${e.option_id}`} className={'productFullDetail__section '}>
                                <h3 className="option__title">
                                    <span>{e.title}</span>{' '}
                                    {e.required && <span className={classes.required_info}> *</span>}
                                </h3>
                                <div>{renderItemOptions(e)}</div>
                            </section>
                        ))}
                        <section className="productFullDetail__section">
                            <h3>
                                <span className={classes.required_info}>
                                    * {t('product:required-fields', 'Required fields')}
                                </span>
                                <span className={classes.customization__info}>
                                    {t('product:customization-price', 'Your customization')}: {getTotalPrice()}
                                </span>
                            </h3>
                        </section>
                        <section className="productFullDetail__quantity productFullDetail__section">
                            <h2 className="productFullDetail__quantityTitle option__title">
                                <span>{t('cart:quantity', 'Quantity')}</span>
                            </h2>
                            <div className="quantity__root">
                                <span className={'fieldIcons__root ' + classes.select_icons}>
                                    <span className="fieldIcons__input">
                                        <select
                                            aria-label="product's quantity"
                                            className="select__input field__input"
                                            name="quantity">
                                            <option value="1">1</option>
                                            <option value="2">2</option>
                                            <option value="3">3</option>
                                            <option value="4">4</option>
                                        </select>
                                    </span>
                                    <span className="fieldIcons__before"></span>
                                    <span className="fieldIcons__after">
                                        <span className="icon__root">
                                            <svg
                                                xmlns="http://www.w3.org/2000/svg"
                                                width="18"
                                                height="18"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="currentColor"
                                                strokeWidth="2"
                                                strokeLinecap="round"
                                                strokeLinejoin="round">
                                                <polyline points="6 9 12 15 18 9"></polyline>
                                            </svg>
                                        </span>
                                    </span>
                                </span>
                                <p className="message-root"></p>
                            </div>
                        </section>
                        <section className="productFullDetail__cartActions productFullDetail__section">
                            <button
                                className="button__root_highPriority button__root clickable__root button__filled"
                                type="button"
                                disabled="">
                                <span className="button__content">
                                    <span>{t('product:add-item', 'Add to Cart')}</span>
                                </span>
                            </button>
                        </section>
                    </>
                )}
            </>,
            optionsContainer
        );
    }

    return <></>;
};

BundleProductOptions.propTypes = {
    container: instanceOf(Element).isRequired
};

export default BundleProductOptions;
