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

import { useStorefrontEvents, LoadingIndicator } from '@adobe/aem-core-cif-react-components';

import { useRecommendations } from '../../hooks/useRecommendations';
import { useVisibilityObserver } from '../../hooks/useVisibilityObserver';

import classes from './ProductRecsGallery.css';
import ProductCard from './ProductCard';

const ProductRecsGallery = props => {
    const mse = useStorefrontEvents();
    const rendered = useRef(false);
    const { loading, units } = useRecommendations(props);
    const { observeElement } = useVisibilityObserver();

    let content = '';

    if (loading) {
        content = <LoadingIndicator />;
    }

    if (units && units.length > 0 && units[0].products.length > 0) {
        const unit = units[0];

        const isVisible = () => {
            mse && mse.publish.recsUnitView(unit.unitId);
        };

        content = (
            <>
                <h2 className={classes.title}>{unit.unitName || props.title}</h2>
                <div className={classes.container} ref={e => observeElement(e, isVisible)}>
                    {unit.products.map(product => (
                        <ProductCard unit={unit} product={product} key={product.sku} />
                    ))}
                </div>
            </>
        );

        if (!rendered.current) {
            mse && mse.publish.recsUnitRender(unit.unitId);
            rendered.current = true;
        }
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
    preconfigured: PropTypes.bool
};

export default ProductRecsGallery;
