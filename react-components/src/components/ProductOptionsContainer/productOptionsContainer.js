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
import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

import BundleProductOptions from './BundleProductOptions';
import CustomizableProductOptions from './CustomizableProductOptions';
import Button from '../Button';
import Price from '../Price';

import { useConfigContext } from '../../context/ConfigContext';
import * as dataLayerUtils from '../../utils/dataLayerUtils';
import * as optionsUtils from './utils';

const ProductOptionsContainer = () => {
    const [t] = useTranslation(['product', 'cart']);

    const [optionsState, setOptionsState] = useState({
        dataFetched: false,
        showCustomizeButton: false,
        productQuantity: 1,
        bundleState: null,
        customizableOptionsState: null
    });

    const { mountingPoints: { productOptionsContainer } } = useConfigContext();

    const sku = document.querySelector(productOptionsContainer)?.dataset?.sku
    const currencyCode = document.querySelector(productOptionsContainer)?.dataset?.currency;
    const rawProductData = document.querySelector(productOptionsContainer)?.dataset?.raw;
    const productId = document.querySelector('[data-cmp-is=product]')?.id;

    const productData = rawProductData ? JSON.parse(rawProductData)?.data?.products?.items[0] : null;

    useEffect(() => {
        if (productData) {
            let changes = { dataFetched: true };

            if (productData.__typename === "BundleProduct") {
                changes['bundleState'] = optionsUtils.createBundleState(productData.items);
                changes['showCustomizeButton'] = true;
            }

            if (productData?.options?.length > 0) {
                changes['customizableOptionsState'] = optionsUtils.createCustomizableOptionsState(productData.options);
            }

            setOptionsState({
                ...optionsState,
                ...changes
            })
        }
    }, []);

    const { dataFetched, showCustomizeButton, productQuantity, bundleState, customizableOptionsState } = optionsState;

    const getTotalPrice = () => {
        let price = 0;
        price += optionsUtils.getBundlePrice(bundleState);
        price += optionsUtils.getCustomizableOptionsPrice(customizableOptionsState);

        return <Price currencyCode={currencyCode} value={price} className="customPrice" />;
    }

    const changeQuantity = e => {
        const { target: { value } } = e;

        setOptionsState({
            ...optionsState,
            productQuantity: value
        });
    }

    const canAddToCart = () => {
        let result = optionsUtils.isOptionsStateValid(bundleState) && optionsUtils.isOptionsStateValid(customizableOptionsState);
        return result;
    }

    const addToCart = () => {
        const cartProduct = {
            sku,
            virtual: false,
            quantity: productQuantity,
            ...optionsUtils.getBundleCartOptions(bundleState),
            ...optionsUtils.getCustomizableCartOptions(customizableOptionsState)
        };

        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [cartProduct]
        });
        document.dispatchEvent(customEvent);
        // https://github.com/adobe/xdm/blob/master/docs/reference/datatypes/productlistitem.schema.md
        dataLayerUtils.pushEvent('cif:addToCart', {
            '@id': productId,
            'xdm:SKU': cartProduct.sku,
            'xdm:quantity': cartProduct.quantity,
            bundle: true
        });
    }

    if (!dataFetched) return <></>;

    if (showCustomizeButton) {
        return (
            <section className="productFullDetail__section productFullDetail__customizeBundle">
                <Button priority="high" onClick={() => setOptionsState({ ...optionsState, showCustomizeButton: false })}>
                    <span>{t('product:customize-bundle', 'Customize')}</span>
                </Button>
            </section>
        );
    }

    return (<>
        {bundleState !== null &&
            <BundleProductOptions
                key={"bundle-options"}
                currencyCode={currencyCode}
                data={bundleState}
                handleBundleChange={(newBundleState) => setOptionsState({ ...optionsState, bundleState: newBundleState })} />}
        {customizableOptionsState !== null &&
            <CustomizableProductOptions
                key={"customizable-options"}
                currencyCode={currencyCode}
                data={customizableOptionsState}
                handleOptionsChange={(newCustomizableOptionsState) => setOptionsState({ ...optionsState, customizableOptionsState: newCustomizableOptionsState })} />}
        <section className="productFullDetail__section productFullDetail__productOption">
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
                            value={productQuantity}
                            onChange={changeQuantity}>
                            <option value="1">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                            <option value="4">4</option>
                        </select>
                    </span>
                    <span className="fieldIcons__before"></span>
                    <span className="fieldIcons__after" >
                        <span className="icon__root" >
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                width="18"
                                height="18"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round" >
                                <polyline points="6 9 12 15 18 9"></polyline>
                            </svg>
                        </span>
                    </span>
                </span>
                <p className="message-root"></p>
            </div>
        </section>
        <section className="productFullDetail__cartActions productFullDetail__section" >
            <button
                className="button__root_highPriority button__root clickable__root button__filled"
                type="button"
                disabled={!canAddToCart()}
                onClick={addToCart} >
                <span className="button__content">
                    <span>{t('product:add-item', 'Add to Cart')}</span>
                </span>
            </button >
        </section >
    </>);
}

export default ProductOptionsContainer;
