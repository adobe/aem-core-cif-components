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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.services.UrlProvider.IdentifierLocation;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;

@ObjectClassDefinition(name = "CIF URL Provider configuration")
public @interface UrlProviderConfiguration {

    String DEFAULT_PRODUCT_URL_TEMPLATE = "${page}.${url_key}.html#${variant_sku}";
    String DEFAULT_CATEGORY_URL_TEMPLATE = "${page}.${id}.html";

    @AttributeDefinition(
        name = "Product URL template",
        description = "Default variables are ${page}, ${sku}, ${variant_sku}, ${url_key} and ${variant_url_key}.",
        type = AttributeType.STRING,
        required = true)
    String productUrlTemplate() default DEFAULT_PRODUCT_URL_TEMPLATE;

    @AttributeDefinition(
        name = "Product identifier location",
        description = "Defines the location of the product identifier in the URL.",
        required = true)
    IdentifierLocation productIdentifierLocation() default IdentifierLocation.SELECTOR;

    @AttributeDefinition(
        name = "Product identifier type",
        description = "Defines the type of the product identifier in the URL.",
        required = true)
    ProductIdentifierType productIdentifierType() default ProductIdentifierType.URL_KEY;

    @AttributeDefinition(
        name = "Category URL template",
        description = "Default variables are ${page}, ${id}, ${url_key} and ${url_path}.",
        type = AttributeType.STRING,
        required = true)
    String categoryUrlTemplate() default DEFAULT_CATEGORY_URL_TEMPLATE;

    @AttributeDefinition(
        name = "Category identifier location",
        description = "Defines the location of the category identifier in the URL.",
        required = true)
    IdentifierLocation categoryIdentifierLocation() default IdentifierLocation.SELECTOR;

    @AttributeDefinition(
        name = "Category identifier type",
        description = "Defines the type of the category identifier in the URL.",
        required = true)
    CategoryIdentifierType categoryIdentifierType() default CategoryIdentifierType.ID;
}
