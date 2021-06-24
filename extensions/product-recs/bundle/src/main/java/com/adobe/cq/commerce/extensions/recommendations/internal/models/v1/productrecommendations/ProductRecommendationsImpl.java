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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.extensions.recommendations.models.common.PriceRange;
import com.adobe.cq.commerce.extensions.recommendations.models.productrecommendations.ProductRecommendations;
import jdk.internal.joptsimple.internal.Strings;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductRecommendations.class, resourceType = ProductRecommendationsImpl.RESOURCE_TYPE)
public class ProductRecommendationsImpl implements ProductRecommendations {

    protected static final String RESOURCE_TYPE = "core/cif/extensions/product-recs/components/productrecommendations/v1/productrecommendations";

    private static final String TITLE_PROP = "jcr:title";

    @Inject
    protected Resource resource;

    private ValueMap props;

    @PostConstruct
    private void initModel() {

        props = resource.adaptTo(ValueMap.class);

    }

    @Override
    public String getTitle() {
        return props.get(TITLE_PROP, Strings.EMPTY);
    }

    @Override
    public String getRecommendationType() {
        return null;
    }

    @Override
    public List<String> getCategoryInclusions() {
        return null;
    }

    @Override
    public List<String> getCategoryExclusions() {
        return null;
    }

    @Override
    public List<String> getProductInclusions() {
        return null;
    }

    @Override
    public List<String> getProductExclusions() {
        return null;
    }

    @Override
    public PriceRange getPriceRangeInclusions() {
        return null;
    }

    @Override
    public PriceRange getPriceRangeExclusions() {
        return null;
    }

    @Override
    public List<String> getTypeInclusions() {
        return null;
    }

    @Override
    public List<String> getTypeExclusions() {
        return null;
    }

    @Override
    public List<String> getVisibilityInclusions() {
        return null;
    }

    @Override
    public List<String> getVisibilityExclusions() {
        return null;
    }

    @Override
    public boolean excludeOutOfStock() {
        return false;
    }

    @Override
    public boolean excludeLowStock() {
        return false;
    }

}