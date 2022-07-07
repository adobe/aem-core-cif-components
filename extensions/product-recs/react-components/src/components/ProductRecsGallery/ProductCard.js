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
import React, { useRef } from 'react';
import PropTypes from 'prop-types';

import { useIntl } from 'react-intl';
import { useStorefrontEvents, Price, Trigger, createProductPageUrl } from '@adobe/aem-core-cif-react-components';

import classes from './ProductCard.css';

const ProductCard = props => {
    const { showAddToWishList } = props;
    const mse = useStorefrontEvents();
    const intl = useIntl();
    const addToCartRef = useRef();
    const addToWishlistRef = useRef();

    const {
        unit: { unitId },
        product: { sku, name, type, productId, currency, prices, smallImage }
    } = props;

    const addToCart = (unit, product) => {
        const { sku, type, productId } = product;
        const { unitId } = unit;

        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            bubbles: true,
            detail: [{ sku, quantity: 1, virtual: type === 'virtual' }]
        });
        addToCartRef.current.dispatchEvent(customEvent);

        mse && mse.publish.recsItemAddToCartClick(unitId, productId);
    };

    const addToWishlist = product => {
        const { sku } = product;

        const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
            bubbles: true,
            detail: [{ sku, quantity: 1 }]
        });
        addToWishlistRef.current.dispatchEvent(customEvent);
    };

    const openDetails = (unit, product) => {
        const { sku, productId } = product;
        const { unitId } = unit;

        window.location.assign(createProductPageUrl(sku));

        mse && mse.publish.recsItemClick(unitId, productId);
    };

    const renderPrice = (prices, currency) => {
        if (!prices || (!prices.minimum && !prices.maximum)) {
            return <></>;
        }

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

    const renderImage = () => {
        if (!smallImage || !smallImage.url) {
            return <></>;
        }
        return <img className={classes.productImage} src={smallImage.url} alt={name} />;
    };

    return (
        <div className={classes.card} key={sku}>
            <a
                href={createProductPageUrl(sku)}
                title={name}
                onClick={() => mse && mse.publish.recsItemClick(unitId, productId)}>
                <div className={classes.cardImage}>{renderImage()}</div>
                <div>{name}</div>
                <div className={classes.price}>{renderPrice(prices, currency)}</div>
            </a>
            {
                <Trigger
                    action={() => {
                        if (['simple', 'virtual', 'downloadable'].includes(type)) {
                            // Add to cart only products that can be added to cart without further customization
                            addToCart(props.unit, props.product);
                        } else {
                            // Open details for other products
                            openDetails(props.unit, props.product);
                        }
                    }}>
                    <span className={classes.addToCart} ref={addToCartRef}>
                        {intl.formatMessage({ id: 'productrecs:add-to-cart', defaultMessage: 'Add to Cart' })}
                    </span>
                </Trigger>
            }
            {showAddToWishList && (
                <Trigger className={classes.buttonMargin} action={() => addToWishlist(props.product)}>
                    <span className={classes.addToWishlist} ref={addToWishlistRef}>
                        {intl.formatMessage({ id: 'productrecs:add-to-wishlist', defaultMessage: 'Add to Wish List' })}
                    </span>
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
    }),
    showAddToWishList: PropTypes.bool
};

export default ProductCard;
