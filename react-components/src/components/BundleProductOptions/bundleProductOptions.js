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
import { useTranslation } from 'react-i18next';
import { useConfigContext } from '../../context/ConfigContext';
import * as dataLayerUtils from '../../utils/dataLayerUtils';
import Checkbox from './checkbox';
import Radio from './radio';
import Select from './select';
import MultiSelect from './multiSelect';

import { useAwaitQuery } from '../../utils/hooks';

import Button from '../Button';

import BUNDLE_PRODUCT_QUERY from '../../queries/query_bundle_product.graphql';
import Price from '../Price';

const BundleProductOptions = () => {
    const [t] = useTranslation(['product', 'cart']);
    const bundleProductQuery = useAwaitQuery(BUNDLE_PRODUCT_QUERY);
    const {
        mountingPoints: { bundleProductOptionsContainer }
    } = useConfigContext();
    const sku = document.querySelector(bundleProductOptionsContainer)?.dataset?.sku;
    const productId = document.querySelector('[data-cmp-is=product]')?.id;
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
                required: item.required,
                title: item.title,
                type: item.type,
                quantity: ['checkbox', 'multi'].includes(item.type) ? 1 : item.options.find(o => o.is_default).quantity,
                options: item.options
                    .slice() // return a shallow copy of the array. the original is frozen and cannot be sorted as Array.prototype.sort() sorts the elements of the array in place
                    .sort((a, b) => a.position - b.position)
                    .map(option => {
                        const {
                            id,
                            can_change_quantity,
                            quantity,
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
                            can_change_quantity,
                            quantity,
                            label,
                            currency,
                            price: value
                        };
                    }),
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
        setBundleState({ selections, currencyCode, quantity: 1 });
    };

    const handleSelectionChange = (option_id, quantity, customization) => {
        const { selections } = bundleState;
        const selectionIndex = selections.findIndex(s => s.option_id === option_id);
        const selection = selections[selectionIndex];
        selections.splice(selectionIndex, 1, { ...selection, quantity, customization });

        setBundleState({ ...bundleState, selections: [...selections] });
    };

    const changeBundleQuantity = e => {
        const { value } = e.target;
        setBundleState({ ...bundleState, quantity: value });
    };

    const renderItemOptions = item => {
        const { option_id, required, quantity, options, type, customization } = item;
        const otherProps = { options, customization, handleSelectionChange };

        switch (type) {
            case 'checkbox': {
                return <Checkbox item={{ option_id, required }} {...otherProps} />;
            }
            case 'radio': {
                return <Radio item={{ option_id, required, quantity }} {...otherProps} />;
            }
            case 'select': {
                return <Select item={{ option_id, required, quantity }} {...otherProps} />;
            }
            case 'multi': {
                return <MultiSelect item={{ option_id, required }} {...otherProps} />;
            }
        }
    };

    const canAddToCart = () => {
        const { selections } = bundleState;
        return selections.reduce((acc, selection) => {
            return acc && (selection.required ? selection.customization.length > 0 : true);
        }, true);
    };

    const addToCart = () => {
        const { selections, quantity } = bundleState;
        const productData = {
            sku,
            virtual: false,
            bundle: true,
            quantity: quantity,
            options: selections.map(s => {
                return {
                    id: s.option_id,
                    quantity: s.quantity,
                    value: s.customization.map(c => c.id.toString())
                };
            })
        };
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [productData]
        });
        document.dispatchEvent(customEvent);
        // https://github.com/adobe/xdm/blob/master/docs/reference/datatypes/productlistitem.schema.md
        dataLayerUtils.pushEvent('cif:addToCart', {
            '@id': productId,
            'xdm:SKU': productData.sku,
            'xdm:quantity': productData.quantity,
            bundle: true
        });
    };

    const getTotalPrice = () => {
        const { selections, currencyCode } = bundleState;
        const price = selections.reduce((acc, selection) => {
            return (
                acc +
                selection.quantity *
                    selection.customization.reduce((a, c) => {
                        return a + c.price * (['checkbox', 'multi'].includes(selection.type) ? c.quantity : 1);
                    }, 0)
            );
        }, 0);

        return <Price currencyCode={currencyCode} value={price} className="bundlePrice" />;
    };

    if (!sku) return <></>;

    if (bundleState === null) {
        return (
            <section className="productFullDetail__section productFullDetail__customizeBundle">
                <Button priority="high" onClick={() => fetchBundleDetails(sku)}>
                    <span>{t('product:customize-bundle', 'Customize')}</span>
                </Button>
            </section>
        );
    }

    return (
        <>
            {bundleState.selections.map(e => (
                <section
                    key={`item-${e.option_id}`}
                    className="productFullDetail__section productFullDetail__bundleProduct">
                    <h3 className="option__title">
                        <span>{e.title}</span> {e.required && <span className="required"> *</span>}
                    </h3>
                    <div>{renderItemOptions(e)}</div>
                </section>
            ))}
            <section className="productFullDetail__section productFullDetail__bundleProduct">
                <h3>
                    <span className="required">* {t('product:required-fields', 'Required fields')}</span>
                    <span className="priceInfo">
                        {t('product:customization-price', 'Your customization')}: {getTotalPrice()}
                    </span>
                </h3>
            </section>
            <section className="productFullDetail__quantity productFullDetail__section">
                <h2 className="productFullDetail__quantityTitle option__title">
                    <span>{t('cart:quantity', 'Quantity')}</span>
                </h2>
                <div className="quantity__root">
                    <span className="fieldIcons__root" style={{ '--iconsBefore': 0, '--iconsAfter': 1 }}>
                        <span className="fieldIcons__input">
                            <select
                                aria-label="product's quantity"
                                className="select__input field__input"
                                name="quantity"
                                value={bundleState.quantity}
                                onChange={changeBundleQuantity}>
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
                    disabled={!canAddToCart()}
                    onClick={addToCart}>
                    <span className="button__content">
                        <span>{t('product:add-item', 'Add to Cart')}</span>
                    </span>
                </button>
            </section>
        </>
    );
};

export default BundleProductOptions;
