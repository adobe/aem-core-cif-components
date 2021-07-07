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

public class MockUrlProviderConfiguration implements Annotation, UrlProviderConfiguration {

    private String productPageUrlFormat = UrlFormat.ProductPageWithUrlKey.PATTERN;
    private String categoryPageUrlFormat = UrlFormat.CategoryPageWithUrlPath.PATTERN;

    public MockUrlProviderConfiguration() {}

    @Override
    public Class<? extends Annotation> annotationType() {
        return UrlProviderConfiguration.class;
    }

    @Override
    public String productPageUrlFormat() {
        return productPageUrlFormat;
    }

    public void setProductPageUrlFormat(String productPageUrlFormat) {
        this.productPageUrlFormat = productPageUrlFormat;
    }

    @Override
    public String categoryPageUrlFormat() {
        return categoryPageUrlFormat;
    }

    public void setCategoryPageUrlFormat(String categoryPageUrlFormat) {
        this.categoryPageUrlFormat = categoryPageUrlFormat;
    }
}
