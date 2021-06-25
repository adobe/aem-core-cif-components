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
package com.adobe.cq.commerce.extensions.recommendations.internal.models.v1.productrecommendations;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.extensions.recommendations.internal.models.v1.common.PriceRangeImpl;
import com.adobe.cq.commerce.extensions.recommendations.models.common.PriceRange;
import com.adobe.cq.commerce.extensions.recommendations.models.productrecommendations.ProductRecommendations;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductRecommendations.class, resourceType = ProductRecommendationsImpl.RESOURCE_TYPE)
public class ProductRecommendationsImpl implements ProductRecommendations {

    protected static final String RESOURCE_TYPE = "core/cif/extensions/product-recs/components/productrecommendations/v1/productrecommendations";

    private static final String TITLE_PROP = "jcr:title";
    private static final String TYPE_PROP = "recommendationType";
    private static final String INCLUDE_CATEGORIES = "includeCategories";
    private static final String INCLUDED_CATEGORIES = "includedCategories";
    private static final String EXCLUDE_CATEGORIES = "excludeCategories";
    private static final String EXCLUDED_CATEGORIES = "excludedCategories";
    private static final String INCLUDE_PRODUCTS = "includeProducts";
    private static final String INCLUDED_PRODUCTS = "includedProducts";
    private static final String EXCLUDE_PRODUCTS = "excludeProducts";
    private static final String EXCLUDED_PRODUCTS = "excludedProducts";
    private static final String INCLUDE_TYPES = "includeTypes";
    private static final String INCLUDED_TYPES = "includedTypes";
    private static final String EXCLUDE_TYPES = "excludeTypes";
    private static final String EXCLUDED_TYPES = "excludedTypes";
    private static final String INCLUDE_VISIBILITY = "excludeVisibility";
    private static final String INCLUDED_VISIBILITY = "excludedVisibility";
    private static final String EXCLUDE_VISIBILITY = "excludeVisibility";
    private static final String EXCLUDED_VISIBILITY = "excludedVisibility";
    private static final String INCLUDE_PRICE_RANGE = "includePriceRange";
    private static final String INCLUDED_MIN_PRICE = "includedMinPrice";
    private static final String INCLUDED_MAX_PRICE = "includedMaxPrice";
    private static final String EXCLUDE_PRICE_RANGE = "excludePriceRange";
    private static final String EXCLUDED_MIN_PRICE = "excludedMinPrice";
    private static final String EXCLUDED_MAX_PRICE = "excludedMaxPrice";
    private static final String EXCLUDE_OUT_OF_STOCK = "excludeOutOfStock";
    private static final String EXCLUDE_LOW_STOCK = "excludeLowStock";

    @Inject
    protected Resource resource;

    private ValueMap props;

    @PostConstruct
    private void initModel() {
        props = resource.adaptTo(ValueMap.class);
    }

    private String getProperty(String enabledProperty, String property) {
        if (props.get(enabledProperty, false)) {
            Object includedCategories = props.get(property);
            if (includedCategories == null) {
                return null;
            }

            if (includedCategories instanceof String[]) {
                return StringUtils.join((String[]) includedCategories, ",");
            } else if (includedCategories instanceof String) {
                return (String) includedCategories;
            }

        }
        return null;
    }

    @Override
    public String getTitle() {
        return props.get(TITLE_PROP, StringUtils.EMPTY);
    }

    @Override
    public String getRecommendationType() {
        return props.get(TYPE_PROP, StringUtils.EMPTY);
    }

    @Override
    public String getCategoryInclusions() {
        return getProperty(INCLUDE_CATEGORIES, INCLUDED_CATEGORIES);
    }

    @Override
    public String getCategoryExclusions() {
        return getProperty(EXCLUDE_CATEGORIES, EXCLUDED_CATEGORIES);
    }

    @Override
    public String getProductInclusions() {
        return getProperty(INCLUDE_PRODUCTS, INCLUDED_PRODUCTS);
    }

    @Override
    public String getProductExclusions() {
        return getProperty(EXCLUDE_PRODUCTS, EXCLUDED_PRODUCTS);
    }

    @Override
    public PriceRange getPriceRangeInclusions() {
        if (props.get(INCLUDE_PRICE_RANGE, false)) {
            return new PriceRangeImpl(props.get(INCLUDED_MIN_PRICE, Long.class),
                    props.get(INCLUDED_MAX_PRICE, Long.class));
        }
        return null;
    }

    @Override
    public PriceRange getPriceRangeExclusions() {
        if (props.get(EXCLUDE_PRICE_RANGE, false)) {
            return new PriceRangeImpl(props.get(EXCLUDED_MIN_PRICE, Long.class),
                    props.get(EXCLUDED_MAX_PRICE, Long.class));
        }
        return null;
    }

    @Override
    public String getTypeInclusions() {
        return getProperty(INCLUDE_TYPES, INCLUDED_TYPES);
    }

    @Override
    public String getTypeExclusions() {
        return getProperty(EXCLUDE_TYPES, EXCLUDED_TYPES);
    }

    @Override
    public String getVisibilityInclusions() {
        return getProperty(INCLUDE_VISIBILITY, INCLUDED_VISIBILITY);
    }

    @Override
    public String getVisibilityExclusions() {
        return getProperty(EXCLUDE_VISIBILITY, EXCLUDED_VISIBILITY);
    }

    @Override
    public boolean excludeOutOfStock() {
        return props.get(EXCLUDE_OUT_OF_STOCK, false);
    }

    @Override
    public boolean excludeLowStock() {
        return props.get(EXCLUDE_LOW_STOCK, false);
    }

}