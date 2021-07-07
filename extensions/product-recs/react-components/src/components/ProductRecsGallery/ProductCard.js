/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
import React from 'react';
import PropTypes from 'prop-types';

import { useTranslation } from 'react-i18next';
import { useStorefrontEvents, Price, Trigger, createProductPageUrl } from '@adobe/aem-core-cif-react-components';

import classes from './ProductCard.css';

const ProductCard = props => {
    const mse = useStorefrontEvents();
    const [t] = useTranslation();

    const {
        unit: { unitId },
        product: { sku, name, type, productId, currency, prices, smallImage }
    } = props;

    const addToCart = (unit, product) => {
        const { sku, type, productId } = product;
        const { unitId } = unit;

        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [{ sku, quantity: 1, virtual: type === 'virtual' }]
        });
        document.dispatchEvent(customEvent);

        mse && mse.publish.recsItemAddToCartClick(unitId, productId);
    };

    const renderPrice = (prices, currency) => {
        const { minimum, maximum } = prices;
        const isRange = !!(Math.round(minimum.final * 100) != Math.round(maximum.final * 100));

        if (isRange) {
            return (
                <>
                    {t('productrecs:price-from', 'from')} <Price value={minimum.final} currencyCode={currency} />
                </>
            );
        }

        return <Price value={minimum.final} currencyCode={currency} />;
    };

    return (
        <div className={classes.card} key={sku}>
            <a href={createProductPageUrl(sku)} onClick={() => mse && mse.publish.recsItemClick(unitId, productId)}>
                <div className={classes.cardImage}>
                    <img className={classes.productImage} src={smallImage.url} alt={name} />
                </div>
                <div>{name}</div>
                <div className={classes.price}>{renderPrice(prices, currency)}</div>
            </a>
            {// Only display add to cart button for products that can be added to cart without further customization
            ['simple', 'virtual', 'downloadable'].includes(type) && (
                <Trigger action={() => addToCart(props.unit, props.product)}>
                    <span className={classes.addToCart}>{t('productrecs:add-to-cart', 'Add to cart')}</span>
                </Trigger>
            )}
        </div>
    );
};

ProductCard.propTypes = {
    unit: PropTypes.shape({
        unitId: PropTypes.string.isRequired
    }),
    product: PropTypes.shape({
        sku: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        type: PropTypes.string,
        productId: PropTypes.number,
        currency: PropTypes.string,
        prices: PropTypes.shape({
            minimum: PropTypes.shape({
                final: PropTypes.number
            }),
            maximum: PropTypes.shape({
                final: PropTypes.number
            })
        }),
        smallImage: PropTypes.shape({
            url: PropTypes.string
        })
    })
};

export default ProductCard;
