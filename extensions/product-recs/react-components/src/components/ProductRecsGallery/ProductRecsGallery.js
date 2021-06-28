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
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import RecommendationsClient from '@magento/recommendations-js-sdk';
import { useStorefrontEvents, Price, Trigger } from '@adobe/aem-core-cif-react-components';

import { useStorefrontInstanceContext } from '../../context/StorefrontInstanceContext';

import classes from './ProductRecsGallery.css';

// TODO: Add npm link to components react components
// TODO: Update version updating for releases

const ProductRecsGallery = props => {
    const storefrontInstance = useStorefrontInstanceContext();

    // TODO: Add all the events
    const mse = useStorefrontEvents();

    const [unit, setUnit] = useState(null);

    useEffect(() => {
        if (!storefrontInstance) {
            return;
        }

        (async () => {
            // If no parameters are passed, everything is automatically taken from MSE
            // TODO: Remove PageType after source branch was updated
            const client = new RecommendationsClient({ alternateEnvironmentId: '', pageType: 'CMS' });

            // TODO: Add filters
            client.register({ name: props.title, type: props.recommendationType });

            const { status, data } = await client.fetch();
            console.log(status, data);

            if (status !== 200 || !data || !data.units || data.units.length === 0) {
                console.error('Could not load product recommendations', status);
            }

            setUnit(data.units[0]);

            // TODO
            // mse.context.setRecommendations({ units: recommendationsContext });
            // mse.publish.recsResponseReceived();
        })();
    }, [storefrontInstance]);

    if (!unit) {
        // TODO Loading spinner
        return <div>Loading...</div>;
    }

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
                ['simple', 'virtual'].includes(product.type) && (
                    <Trigger action={() => addToCart(product)}>
                        <span className={classes.addToCart}>Add to cart</span>
                    </Trigger>
                )}
            </div>
        );
    };

    return (
        <div className={classes.root}>
            <h2 className={classes.title}>{props.title}</h2>
            <div className={classes.container}>{unit.products.map(renderCard)}</div>
        </div>
    );
};

ProductRecsGallery.propTypes = {
    title: PropTypes.string.isRequired,
    recommendationType: PropTypes.string.isRequired
};

export default ProductRecsGallery;
