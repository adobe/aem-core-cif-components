/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "CIF GraphQL Category Cache Configuration for the Navigation Component")
public @interface CategoryCacheConfig {
    boolean DEFAULT_ENABLED = true;
    int DEFAULT_MAX_SIZE = 20;
    int DEFAULT_EXPIRATION_MINUTES = 5;

    @AttributeDefinition(
        name = "Enable/disable category caching",
        description = "Enables/disables the caching of the categories in the Navigation component",
        type = AttributeType.BOOLEAN)
    boolean enabled() default DEFAULT_ENABLED;

    @AttributeDefinition(
        name = "Maximum cache size (= maximum number of categories in the cache)",
        description = "The maximum size of the categories cache, driven by the expected maximum number of "
            + "pages of type 'Catalog Page' with distinct categories.",
        type = AttributeType.INTEGER)
    int maxSize() default DEFAULT_MAX_SIZE;

    @AttributeDefinition(
        name = "Cache expiration time in minutes",
        description = "The maximum amount of time in minutes while a category is retained in the cache",
        type = AttributeType.INTEGER)
    int expirationMinutes() default DEFAULT_EXPIRATION_MINUTES;
}
