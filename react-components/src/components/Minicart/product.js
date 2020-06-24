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

    const { product = {}, quantity = 0, id = '', prices } = item;
    const { thumbnail, name } = product;
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
        removeItem(id);
    }, [id, removeItem]);

    return (
        <li className={classes.root} data-testid="cart-item">
            {productImage}
            <div className={classes.name}>{name}</div>
            <div className={classes.quantity}>
                <div className={classes.quantityRow}>
                    <span>{quantity}</span>
                    <span className={classes.quantityOperator}>{'×'}</span>
                    <span className={classes.price}>
                        <Price currencyCode={price.currency} value={price.value} />
                    </span>
                </div>
                <div className={classes.rowTotalRow}>
                    <span className={classes.rowTotal}>
                        <Price currencyCode={row_total.currency} value={row_total.value} />
                    </span>
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
        id: string.isRequired,
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
