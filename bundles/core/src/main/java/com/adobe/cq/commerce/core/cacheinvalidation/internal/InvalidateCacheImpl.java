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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;

@Component(service = InvalidateCacheImpl.class, immediate = true)
public class InvalidateCacheImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateCacheImpl.class);
    private static final String DEFAULT_STORE_VIEW = "default";

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    @Reference
    private InvalidateCacheRegistry invalidateCacheRegistry;

    public void invalidateCache(String path) {
        try (ResourceResolver resourceResolver = invalidateCacheSupport.getServiceUserResourceResolver()) {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                processResource(resourceResolver, resource);
            } else {
                LOGGER.debug("Resource not found at path: {}", path);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing JCR event: {}", e.getMessage(), e);
        }
    }

    private void processResource(ResourceResolver resourceResolver, Resource resource) {
        String storePath = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
        ComponentsConfiguration commerceProperties = invalidateCacheSupport.getCommerceProperties(resourceResolver, storePath);
        if (commerceProperties != null) {
            handleCacheInvalidation(resource, commerceProperties);
        } else {
            LOGGER.debug("Commerce data not found at path: {}", resource.getPath());
        }
    }

    private void handleCacheInvalidation(Resource resource, ComponentsConfiguration commerceProperties) {
        String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, String.class);
        String storeView = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_STORE_VIEW, DEFAULT_STORE_VIEW);

        GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);
        ValueMap properties = resource.getValueMap();

        String[] listOfCacheToSearch = properties.get(InvalidateCacheSupport.PROPERTIES_CACHE_NAME, String[].class);

        Map<String, String[]> dynamicProperties = getDynamicProperties(properties);
        invalidateCacheByType(client, storeView, listOfCacheToSearch, dynamicProperties);
    }

    private Map<String, String[]> getDynamicProperties(ValueMap properties) {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        for (String attribute : invalidateCacheRegistry.getAttributes()) {
            String[] values = properties.get(attribute, String[].class);
            if (values != null) {
                dynamicProperties.put(attribute, values);
            }
        }
        return dynamicProperties;
    }

    private void invalidateCacheByType(GraphqlClient client, String storeView, String[] listOfCacheToSearch,
        Map<String, String[]> dynamicProperties) {
        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            if (values != null && values.length > 0) {
                String[] cachePatterns = getAttributePatterns(values, key);
                client.invalidateCache(storeView, listOfCacheToSearch, cachePatterns);
            }
        }
    }

    private String[] getAttributePatterns(String[] patterns, String attribute) {
        String pattern = invalidateCacheRegistry.getPattern(attribute);
        if (pattern == null) {
            return patterns;
        }
        String attributeString = String.join("|", patterns);
        return new String[] { pattern + "(" + attributeString + ")" };
    }
}
