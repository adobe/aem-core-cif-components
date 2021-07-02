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
import React, { useRef } from 'react';
import PropTypes from 'prop-types';

import { useTranslation } from 'react-i18next';
import {
    useStorefrontEvents,
    Price,
    Trigger,
    LoadingIndicator,
    createProductPageUrl
} from '@adobe/aem-core-cif-react-components';

import { useRecommendations } from '../../hooks/useRecommendations';
import { useVisibilityObserver } from '../../hooks/useVisibilityObserver';

import classes from './ProductRecsGallery.css';

const ProductRecsGallery = props => {
    const mse = useStorefrontEvents();
    const rendered = useRef(false);
    const { loading, data } = useRecommendations(props);
    const { observeElement } = useVisibilityObserver();
    const [t] = useTranslation();

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

    const renderCard = (unit, product) => {
        const { unitId } = unit;
        const { sku, name, type, productId, currency, prices, smallImage } = product;

        return (
            <div className={classes.card} key={sku}>
                <a href={createProductPageUrl(sku)} onClick={() => mse && mse.publish.recsItemClick(unitId, productId)}>
                    <div className={classes.cardImage}>
                        <img className={classes.productImage} src={smallImage.url} alt={name} />
                    </div>
                    <div>{product.name}</div>
                    <div className={classes.price}>{renderPrice(prices, currency)}</div>
                </a>
                {// Only display add to cart button for products that can be added to cart without further customization
                ['simple', 'virtual', 'downloadable'].includes(type) && (
                    <Trigger action={() => addToCart(unit, product)}>
                        <span className={classes.addToCart}>{t('productrecs:add-to-cart', 'Add to cart')}</span>
                    </Trigger>
                )}
            </div>
        );
    };

    let content = '';

    if (loading) {
        content = <LoadingIndicator />;
    }

    if (data) {
        const unit = data.units[0];
        if (unit.products.length > 0) {
            const isVisible = () => {
                mse && mse.publish.recsUnitView(unit.unitId);
            };

            content = (
                <>
                    <h2 className={classes.title}>{props.title}</h2>
                    <div className={classes.container} ref={e => observeElement(e, isVisible)}>
                        {unit.products.map(p => renderCard(unit, p))}
                    </div>
                </>
            );

            if (!rendered.current) {
                mse && mse.publish.recsUnitRender(unit.unitId);
                rendered.current = true;
            }
        }
    }

    return <div className={classes.root}>{content}</div>;
};

ProductRecsGallery.propTypes = {
    title: PropTypes.string.isRequired,
    recommendationType: PropTypes.string.isRequired,
    categoryExclusions: PropTypes.string,
    categoryInclusions: PropTypes.string,
    excludeMaxPrice: PropTypes.string,
    excludeMinPrice: PropTypes.string,
    includeMaxPrice: PropTypes.string,
    includeMinPrice: PropTypes.string
};

export default ProductRecsGallery;
