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
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';

import { LoadingIndicator } from '@adobe/aem-core-cif-react-components';

import { useStorefrontInstanceContext } from '../../context/StorefrontInstanceContext';
import { useRecommendations } from '../../hooks/useRecommendations';
import { useVisibilityObserver } from '../../hooks/useVisibilityObserver';

import classes from './ProductRecsGallery.css';
import ProductCard from './ProductCard';

const ProductRecsGallery = props => {
    const { hostElement, cmpDataLayer } = props;
    const { mse } = useStorefrontInstanceContext();
    const { showAddToWishList } = props;
    const { loading, units } = useRecommendations(props);
    const { observeElement } = useVisibilityObserver();
    const unit = units && units.length > 0 && units[0];

    let content = '';

    useEffect(() => {
        if (!loading && hostElement) {
            const products = unit?.products || [];
            hostElement.dispatchEvent(
                new CustomEvent('aem.cif.product-recs-loaded', {
                    bubbles: true,
                    detail: products
                })
            );
        }
    }, [loading, unit]);

    useEffect(() => {
        if (unit) {
            mse && mse.publish.recsUnitRender(unit.unitId);
        }
    }, [mse, units]);

    if (loading) {
        content = <LoadingIndicator />;
    }

    if (units && units.length > 0 && units[0].products.length > 0) {
        let parentId;
        const unit = units[0];

        const isVisible = () => {
            mse && mse.publish.recsUnitView(unit.unitId);
        };

        if (cmpDataLayer) {
            const dataLayer = JSON.parse(cmpDataLayer);
            parentId = Object.keys(dataLayer)[0];
        }

        content = (
            <>
                <h2 className={classes.title}>{unit.storefrontLabel || props.title || unit.unitName}</h2>
                <div className={classes.container} ref={e => observeElement(e, isVisible)}>
                    {unit.products.map(product => (
                        <ProductCard
                            parentId={parentId}
                            unit={unit}
                            product={product}
                            key={product.sku}
                            showAddToWishList={showAddToWishList}
                        />
                    ))}
                </div>
            </>
        );
    }

    return <div className={classes.root}>{content}</div>;
};

ProductRecsGallery.propTypes = {
    title: PropTypes.string,
    recommendationType: PropTypes.string,
    categoryExclusions: PropTypes.string,
    categoryInclusions: PropTypes.string,
    excludeMaxPrice: PropTypes.string,
    excludeMinPrice: PropTypes.string,
    includeMaxPrice: PropTypes.string,
    includeMinPrice: PropTypes.string,
    preconfigured: PropTypes.bool,
    showAddToWishList: PropTypes.bool,
    hostElement: PropTypes.instanceOf(HTMLElement),
    cmpDataLayer: PropTypes.string
};

export default ProductRecsGallery;
