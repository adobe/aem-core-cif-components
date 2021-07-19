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

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;

@ObjectClassDefinition(name = "CIF URL Provider configuration")
@ProviderType
public @interface UrlProviderConfiguration {

    @AttributeDefinition(
        name = "Product page url format",
        description = "Defines the format of a product page URL.",
        options = {
            @Option(
                label = "Suffix with product sku : " + ProductPageWithSku.PATTERN,
                value = ProductPageWithSku.PATTERN),
            @Option(
                label = "Suffix with product sku & url_key : " + ProductPageWithSkuAndUrlKey.PATTERN,
                value = ProductPageWithSkuAndUrlKey.PATTERN),
            @Option(
                label = "Suffix with product sku & url_path : " + ProductPageWithSkuAndUrlPath.PATTERN,
                value = ProductPageWithSkuAndUrlPath.PATTERN),
            @Option(
                label = "Suffix with product url_key : " + ProductPageWithUrlKey.PATTERN,
                value = ProductPageWithUrlKey.PATTERN),
            @Option(
                label = "Suffix with product url_path : " + ProductPageWithUrlPath.PATTERN,
                value = ProductPageWithUrlPath.PATTERN)
        })
    String productPageUrlFormat() default ProductPageWithUrlKey.PATTERN;

    @AttributeDefinition(
        name = "Category page url format",
        description = "Defines the format of a category page URL.",
        options = {
            @Option(
                label = "Suffix with product url_key : " + CategoryPageWithUrlKey.PATTERN,
                value = CategoryPageWithUrlKey.PATTERN),
            @Option(
                label = "Suffix with product url_path : " + CategoryPageWithUrlPath.PATTERN,
                value = CategoryPageWithUrlPath.PATTERN)
        })
    String categoryPageUrlFormat() default CategoryPageWithUrlPath.PATTERN;
}
