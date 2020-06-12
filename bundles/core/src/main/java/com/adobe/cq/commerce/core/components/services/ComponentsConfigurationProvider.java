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

package com.adobe.cq.commerce.core.components.services;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

public interface ComponentsConfigurationProvider {

    /**
     * Retrieves the context configuration properties for a resource at a specific path.
     *
     * @param path the path of the resource
     * @return a {@link ValueMap} containing the context configuration. If the resource at the specified path doesn't have a context
     *         configuration then this method returns an empty ValueMap
     */
    ValueMap getContextAwareConfigurationProperties(String path);

    /**
     * Retrives the context configuration resource for a resource at a specific path
     * 
     * @param path the path of the resource
     * @return a {@link Resource} representing the context configuration. If the resource at the supplied path doesn't have a context
     *         configuration then this method returns {@code null}
     */
    Resource getContextConfigurationResource(String path);
}
