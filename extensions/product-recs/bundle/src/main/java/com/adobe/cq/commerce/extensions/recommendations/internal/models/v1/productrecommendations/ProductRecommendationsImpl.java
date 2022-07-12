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
package com.adobe.cq.commerce.extensions.recommendations.internal.models.v1.productrecommendations;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.extensions.recommendations.internal.models.v1.common.PriceRangeImpl;
import com.adobe.cq.commerce.extensions.recommendations.models.common.PriceRange;
import com.adobe.cq.commerce.extensions.recommendations.models.productrecommendations.ProductRecommendations;
import com.adobe.cq.wcm.core.components.models.Component;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ProductRecommendations.class,
    resourceType = ProductRecommendationsImpl.RESOURCE_TYPE)
public class ProductRecommendationsImpl implements ProductRecommendations {

    protected static final String RESOURCE_TYPE = "core/cif/extensions/product-recs/components/productrecommendations/v1/productrecommendations";

    private static final String TITLE_PROP = "jcr:title";
    private static final String TYPE_PROP = "recommendationType";
    private static final String PRECONFIGURED_PROP = "preconfigured";
    private static final String INCLUDED_CATEGORIES = "includedCategories";
    private static final String EXCLUDED_CATEGORIES = "excludedCategories";
    private static final String INCLUDED_PRICE_RANGE = "includedPriceRange";
    private static final String INCLUDED_MIN_PRICE = "includedPriceRangeMin";
    private static final String INCLUDED_MAX_PRICE = "includedPriceRangeMax";
    private static final String EXCLUDED_PRICE_RANGE = "excludedPriceRange";
    private static final String EXCLUDED_MIN_PRICE = "excludedPriceRangeMin";
    private static final String EXCLUDED_MAX_PRICE = "excludedPriceRangeMax";
    private static final String DEFAULT_TITLE = "Recommended products";
    private static final String USED_FILTER = "usedFilter";
    private static final String PN_STYLE_ENABLE_ADD_TO_WISHLIST = "enableAddToWishList";
    private static final String PN_CONFIG_ENABLE_WISH_LISTS = "enableWishLists";

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Style currentStyle;
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;
    @Self
    @Via("resource")
    private ValueMap props;
    @Self
    private Component component;

    @Override
    public String getId() {
        return component.getId();
    }

    @Override
    public ComponentData getData() {
        return component.getData();
    }

    @Override
    public String getAppliedCssClasses() {
        return component.getAppliedCssClasses();
    }

    @Override
    public String getExportedType() {
        return component.getExportedType();
    }

    private String getStringListProperty(String propertyName) {
        Object property = props.get(propertyName);
        if (property == null) {
            return null;
        }

        if (property instanceof String[]) {
            return StringUtils.join((String[]) property, ",");
        } else if (property instanceof String) {
            return (String) property;
        }

        return null;
    }

    @Override
    public boolean getPreconfigured() {
        return props.get(PRECONFIGURED_PROP, true);
    }

    @Override
    public String getTitle() {
        if (getPreconfigured()) {
            return null;
        }
        return props.get(TITLE_PROP, DEFAULT_TITLE);
    }

    @Override
    public String getRecommendationType() {
        if (getPreconfigured()) {
            return null;
        }
        return props.get(TYPE_PROP, StringUtils.EMPTY);
    }

    @Override
    public String getCategoryInclusions() {
        if (getPreconfigured() || !props.get(USED_FILTER, StringUtils.EMPTY).equals("./" + INCLUDED_CATEGORIES)) {
            return null;
        }
        return getStringListProperty(INCLUDED_CATEGORIES);
    }

    @Override
    public String getCategoryExclusions() {
        if (getPreconfigured() || !props.get(USED_FILTER, StringUtils.EMPTY).equals("./" + EXCLUDED_CATEGORIES)) {
            return null;
        }
        return getStringListProperty(EXCLUDED_CATEGORIES);
    }

    @Override
    public PriceRange getPriceRangeInclusions() {
        if (getPreconfigured() || !props.get(USED_FILTER, StringUtils.EMPTY).equals("./" + INCLUDED_PRICE_RANGE)) {
            return null;
        }

        return new PriceRangeImpl(props.get(INCLUDED_MIN_PRICE, Double.class),
            props.get(INCLUDED_MAX_PRICE, Double.class));
    }

    @Override
    public PriceRange getPriceRangeExclusions() {
        if (getPreconfigured() || !props.get(USED_FILTER, StringUtils.EMPTY).equals("./" + EXCLUDED_PRICE_RANGE)) {
            return null;
        }

        return new PriceRangeImpl(props.get(EXCLUDED_MIN_PRICE, Double.class),
            props.get(EXCLUDED_MAX_PRICE, Double.class));
    }

    @Override
    public boolean getAddToWishListEnabled() {
        Resource configResource = currentPage != null ? currentPage.getContentResource() : null;
        ComponentsConfiguration configProperties = configResource != null ? configResource.adaptTo(ComponentsConfiguration.class) : null;
        Boolean defaultValue = ProductRecommendations.super.getAddToWishListEnabled();
        return (configProperties != null ? configProperties.get(PN_CONFIG_ENABLE_WISH_LISTS, Boolean.TRUE) : Boolean.TRUE)
            && currentStyle != null ? currentStyle.get(PN_STYLE_ENABLE_ADD_TO_WISHLIST, defaultValue) : defaultValue;
    }
}
