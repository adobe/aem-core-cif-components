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
package com.adobe.cq.commerce.core.components.internal.models.v1;

import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;

/**
 * Interface to implement by modules that need to provide a list of AEM assets used by CIF components
 */
public interface AssetsProvider {

    /**
     * Returns true if the asset provider can handle the provided resource
     *
     * @return Boolean
     */
    boolean canHandle(@Nonnull Resource resource);

    /**
     * Adds assets path to an existing set
     *
     * @param resource the resource for which we retrieve the assets
     * @param assetPaths the existing set of asset paths
     */
    void addAssetPaths(@Nonnull Resource resource, @Nonnull Set<String> assetPaths);
}
