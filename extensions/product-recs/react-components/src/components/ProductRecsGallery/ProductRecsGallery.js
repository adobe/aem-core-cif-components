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

import { useStorefrontEvents, Price, Trigger, LoadingIndicator } from '@adobe/aem-core-cif-react-components';

import classes from './ProductRecsGallery.css';
import { useRecommendations } from '../../hooks/useRecommendations';

// TODO: Add npm link to components react components
// TODO: Update version updating for releases

const ProductRecsGallery = props => {
    // TODO: Add all the events
    const mse = useStorefrontEvents();
    const { loading, data } = useRecommendations(props);

    const addToCart = product => {
        const { sku, type } = product;
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [{ sku, quantity: 1, virtual: type === 'virtual' }]
        });
        document.dispatchEvent(customEvent);
    };

    const renderCard = product => {
        return (
            <div className={classes.card} key={product.sku}>
                <div className={classes.cardImage}>
                    <img className={classes.productImage} src={product.smallImage.url} alt={product.name} />
                </div>
                <div>{product.name}</div>
                <div className={classes.price}>
                    <Price value={product.prices.minimum.final} currencyCode={product.currency} />
                </div>
                {// Only display add to cart button for products that can be added to cart without further customization
                ['simple', 'virtual', 'downloadable'].includes(product.type) && (
                    <Trigger action={() => addToCart(product)}>
                        <span className={classes.addToCart}>Add to cart</span>
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
            content = (
                <>
                    <h2 className={classes.title}>{props.title}</h2>
                    <div className={classes.container}>{unit.products.map(renderCard)}</div>
                </>
            );
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
