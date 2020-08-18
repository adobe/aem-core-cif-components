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
package com.adobe.cq.commerce.core.components.internal.models.v1.productteaser;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.internal.models.v1.AssetsProvider;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;

public class ProductTeaserAssetsProvider implements AssetsProvider {

    public boolean canHandle(@Nonnull Resource resource) {
        return resource.isResourceType(ProductTeaserImpl.RESOURCE_TYPE);
    }

    public void addAssetPaths(@Nonnull Resource resource, @Nonnull List<String> assetPaths) {
        ProductTeaser productTeaser = canHandle(resource) ? resource.adaptTo(ProductTeaser.class) : null;
        if (productTeaser != null) {
            assetPaths.add(productTeaser.getImage());
        }
    }
}
