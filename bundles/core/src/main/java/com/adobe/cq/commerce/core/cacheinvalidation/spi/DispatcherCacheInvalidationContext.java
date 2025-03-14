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

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.day.cq.wcm.api.Page;

/**
 * Context object containing all parameters needed for dispatcher cache invalidation.
 */
public class DispatcherCacheInvalidationContext {
    private final Page page;
    private final ResourceResolver resourceResolver;
    private final Map.Entry<String, String[]> attributeData;
    private final String storePath;
    private final GraphqlClient graphqlClient;

    /**
     * Creates a new DispatcherCacheInvalidationContext with the given parameters.
     *
     * @param page the page
     * @param resourceResolver the resource resolver
     * @param attributeData the attribute data entry
     * @param storePath the store path
     * @param graphqlClient the GraphQL client
     */
    public DispatcherCacheInvalidationContext(Page page, ResourceResolver resourceResolver, Map.Entry<String, String[]> attributeData,
                                              String storePath, GraphqlClient graphqlClient) {
        this.page = page;
        this.resourceResolver = resourceResolver;
        this.attributeData = attributeData;
        this.storePath = storePath;
        this.graphqlClient = graphqlClient;
    }

    /**
     * @return the page
     */
    public Page getPage() {
        return page;
    }

    /**
     * @return the resource resolver
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * @return the attribute data entry
     */
    public Map.Entry<String, String[]> getAttributeData() {
        return attributeData;
    }

    /**
     * @return the store path
     */
    public String getStorePath() {
        return storePath;
    }

    /**
     * @return the GraphQL client
     */
    public GraphqlClient getGraphqlClient() {
        return graphqlClient;
    }
}