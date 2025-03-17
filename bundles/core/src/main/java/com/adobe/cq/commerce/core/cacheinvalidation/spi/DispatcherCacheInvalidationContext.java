/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.cacheinvalidation.spi;

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.day.cq.wcm.api.Page;

/**
 * Context object containing all parameters needed for dispatcher cache invalidation.
 */
@ProviderType
public interface DispatcherCacheInvalidationContext {
    /**
     * @return the page
     */
    Page getPage();

    /**
     * @return the resource resolver
     */
    ResourceResolver getResourceResolver();

    /**
     * @return {@code List<String>} attribute data
     */
    List<String> getAttributeData();

    /**
     * @return the store path
     */
    String getStorePath();

    /**
     * @return the Magento GraphQL client
     */
    MagentoGraphqlClient getGraphqlClient();
}
