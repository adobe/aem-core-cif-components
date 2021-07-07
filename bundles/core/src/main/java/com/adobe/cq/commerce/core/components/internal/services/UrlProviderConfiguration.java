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
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;

@ObjectClassDefinition(name = "CIF URL Provider configuration")
public @interface UrlProviderConfiguration {

    // TODO these should be dropdown, add "options" to annotation

    @AttributeDefinition(
        name = "Product page url format")
    String productPageUrlFormat() default ProductPageWithUrlKey.PATTERN;

    @AttributeDefinition(
        name = "Category page url format")
    String categoryPageUrlFormat() default CategoryPageWithUrlPath.PATTERN;
}
