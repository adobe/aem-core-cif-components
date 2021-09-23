/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
import React from 'react';
import PropTypes from 'prop-types';

import { useIntl } from 'react-intl';
import { useStorefrontEvents, Price, AddToCart, createProductPageUrl } from '@adobe/aem-core-cif-react-components';

import classes from './ProductCard.css';

const ProductCard = props => {
    const mse = useStorefrontEvents();
    const intl = useIntl();

    const {
        unit: { unitId },
        product: { sku, name, type, productId, currency, prices, smallImage }
    } = props;
    const item = {
        sku: sku,
        quantity: 1,
        virtual: type === 'virtual'
    }

    const addToCart = (items) => {
        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: items
        });
        document.dispatchEvent(customEvent);

        mse && mse.publish.recsItemAddToCartClick(unitId, productId);
    };

    const renderPrice = () => {
        const { minimum, maximum } = prices;
        const isRange = Math.round(minimum.final * 100) !== Math.round(maximum.final * 100);

        if (isRange) {
            return (
                <>
                    {intl.formatMessage({ id: 'productrecs:price-from', defaultMessage: 'from' })}{' '}
                    <Price value={minimum.final} currencyCode={currency} />
                </>
            );
        }

        return <Price value={minimum.final} currencyCode={currency} />;
    };

    return (
        <div className={classes.card} key={sku}>
            <a
                href={createProductPageUrl(sku)}
                title={name}
                onClick={() => mse && mse.publish.recsItemClick(unitId, productId)}>
                <div className={classes.cardImage}>
                    <img className={classes.productImage} src={smallImage.url} alt={name} />
                </div>
                <div>{name}</div>
                <div className={classes.price}>{renderPrice()}</div>
            </a>
            {// Only display add to cart button for products that can be added to cart without further customization
                ['simple', 'virtual', 'downloadable'].includes(type) && (
                    <AddToCart items={[item]} onAddToCart={addToCart}>
                        <span>
                            {intl.formatMessage({ id: 'productrecs:add-to-cart', defaultMessage: 'Add to cart' })}
                        </span>
                    </AddToCart>
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
