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
import React, { useMemo, useCallback } from 'react';
import { number, shape, object, string } from 'prop-types';
import { useTranslation } from 'react-i18next';

import classes from './product.css';

import Price from '../Price';
import { transparentPlaceholder } from '../../utils/transparentPlaceholder';
import makeUrl from '../../utils/makeUrl';
import Kebab from './kebab';
import Section from './section';

import useProduct from './useProduct';

const imageWidth = 80;
const imageHeight = 100;

const Product = props => {
    const { item } = props;
    const [t] = useTranslation('cart');

    const { product = {}, quantity = 0, uid = '', prices, bundle_options = [] } = item;
    const { thumbnail, name, __typename } = product;
    const [, { removeItem, editItem }] = useProduct({ item });

    let { price, row_total } = prices;

    const productImage = useMemo(() => {
        const src =
            thumbnail && thumbnail.url
                ? makeUrl(thumbnail.url, { type: 'image-product', width: imageWidth, height: imageHeight })
                : transparentPlaceholder;
        return <img alt={name} className={classes.image} placeholder={transparentPlaceholder} src={src} />;
    });

    const handleRemoveItem = useCallback(() => {
        removeItem(uid);
    }, [uid, removeItem]);

    return (
        <li className={classes.root} data-testid="cart-item">
            {productImage}
            <div className={classes.name}>{name}</div>
            {__typename === 'BundleProduct' && (
                <div className={classes.bundleOptions}>
                    {bundle_options.map(o => (
                        <div key={`${o.id}`}>
                            <p className={classes.bundleOptionTitle}>{o.label}:</p>
                            {o.values.map(v => (
                                <p key={`${o.id}-${v.id}`} className={classes.bundleOptionValue}>
                                    {`${v.quantity} x ${v.label} `}
                                    <Price currencyCode={price.currency} value={v.price} />
                                </p>
                            ))}
                        </div>
                    ))}
                </div>
            )}
            <div className={classes.quantity}>
                <div className={classes.quantityRow}>
                    <span>{quantity}</span>
                    <span className={classes.quantityOperator}>{'×'}</span>
                    <Price className={classes.price} currencyCode={price.currency} value={price.value} />
                </div>
                <div className={classes.rowTotalRow}>
                    <Price className={classes.rowTotal} currencyCode={row_total.currency} value={row_total.value} />
                </div>
            </div>
            <Kebab>
                <Section text={t('cart:edit-item', 'Edit item')} onClick={editItem} icon="Edit2" />
                <Section text={t('cart:remove-item', 'Remove item')} onClick={handleRemoveItem} icon="Trash" />
            </Kebab>
        </li>
    );
};

Product.propTypes = {
    item: shape({
        uid: string.isRequired,
        quantity: number.isRequired,
        prices: shape({
            price: shape({
                value: number.isRequired,
                currency: string.isRequired
            }).isRequired,
            row_total: shape({
                value: number.isRequired,
                currency: string.isRequired
            }).isRequired
        }).isRequired,
        product: shape({
            name: string.isRequired,
            image: object
        })
    })
};

export default Product;
