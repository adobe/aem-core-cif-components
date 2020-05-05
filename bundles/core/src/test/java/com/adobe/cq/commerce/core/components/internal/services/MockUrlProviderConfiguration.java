/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.services;

import java.lang.annotation.Annotation;

import org.apache.commons.lang3.ObjectUtils;

import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.services.UrlProvider.IdentifierLocation;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;

public class MockUrlProviderConfiguration implements Annotation, UrlProviderConfiguration {

    private IdentifierLocation productIdentifierLocation;
    private IdentifierLocation categoryIdentifierLocation;
    private ProductIdentifierType productIdentifierType;
    private CategoryIdentifierType categoryIdentifierType;

    @Override
    public String productUrlTemplate() {
        return UrlProviderConfiguration.DEFAULT_PRODUCT_URL_TEMPLATE;
    }

    @Override
    public IdentifierLocation productIdentifierLocation() {
        return ObjectUtils.firstNonNull(productIdentifierLocation, IdentifierLocation.SELECTOR);
    }

    @Override
    public ProductIdentifierType productIdentifierType() {
        return ObjectUtils.firstNonNull(productIdentifierType, ProductIdentifierType.URL_KEY);
    }

    @Override
    public String categoryUrlTemplate() {
        return UrlProviderConfiguration.DEFAULT_CATEGORY_URL_TEMPLATE;
    }

    @Override
    public IdentifierLocation categoryIdentifierLocation() {
        return ObjectUtils.firstNonNull(categoryIdentifierLocation, IdentifierLocation.SELECTOR);
    }

    @Override
    public CategoryIdentifierType categoryIdentifierType() {
        return ObjectUtils.firstNonNull(categoryIdentifierType, CategoryIdentifierType.ID);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return UrlProviderConfiguration.class;
    }

    public void setProductIdentifierLocation(IdentifierLocation productIdentifierLocation) {
        this.productIdentifierLocation = productIdentifierLocation;
    }

    public void setCategoryIdentifierLocation(IdentifierLocation categoryIdentifierLocation) {
        this.categoryIdentifierLocation = categoryIdentifierLocation;
    }

    public void setProductIdentifierType(ProductIdentifierType productIdentifierType) {
        this.productIdentifierType = productIdentifierType;
    }

    public void setCategoryIdentifierType(CategoryIdentifierType categoryIdentifierType) {
        this.categoryIdentifierType = categoryIdentifierType;
    }
}
