/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.models.categorylist;

import javax.annotation.Nullable;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;

/**
 * An item from a category list
 */
@ConsumerType
public interface FeaturedCategoryListItem {

    /**
     * The identifier of this category.
     * 
     * @return a {@link CommerceIdentifier} object to idenfity this category
     */
    CommerceIdentifier getCategoryIdentifier();

    /**
     * The path to the asset which overrides the default asset.
     * 
     * @return a String representing the AEM path to the asset, or {@code null} if there's no such asset set.
     */
    @Nullable
    String getAssetPath();

}
