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

import java.util.*;

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
                String storePath = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
                ComponentsConfiguration commerceProperties = invalidateCacheSupport.getCommerceProperties(resourceResolver, storePath);
                if (commerceProperties != null) {
                    handleCacheInvalidation(resource, commerceProperties);
                } else {
                    LOGGER.debug("Commerce data not found at path: {}", resource.getPath());
                }
            } else {
                LOGGER.debug("Resource not found at path: {}", path);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing JCR event: {}", e.getMessage(), e);
        }
    }

    private void handleCacheInvalidation(Resource resource, ComponentsConfiguration commerceProperties) {
        String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, String.class);
        if (graphqlClientId == null) {
            LOGGER.debug("GraphQL client ID not found in commerce properties");
            return;
        }

        GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);
        if (client == null) {
            LOGGER.debug("GraphQL client not found for ID: {}", graphqlClientId);
            return;
        }

        ValueMap properties = resource.getValueMap();
        String storeView = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_STORE_VIEW, DEFAULT_STORE_VIEW);

        // Check for InvalidateALL property
        Boolean invalidateAll = properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, Boolean.class);
        if (Boolean.TRUE.equals(invalidateAll)) {
            LOGGER.debug("Performing full cache invalidation");
            invalidateFullCache(client, storeView);
            return;
        }

        String[] listOfCacheToSearch = properties.get(InvalidateCacheSupport.PROPERTIES_CACHE_NAMES, String[].class);
        if (listOfCacheToSearch != null && listOfCacheToSearch.length != 0) {
            LOGGER.debug("Cache invalidation based on cache names");
            client.invalidateCache(storeView, listOfCacheToSearch, new String[0]);
        }

        Map<String, String[]> dynamicProperties = getDynamicProperties(properties);
        if (dynamicProperties.isEmpty()) {
            LOGGER.debug("No dynamic properties found for cache invalidation");
        } else {
            LOGGER.debug("Cache invalidation based on dynamic properties");
            invalidateCacheByType(client, storeView, dynamicProperties);
        }
    }

    private void invalidateFullCache(GraphqlClient client, String storeView) {
        try {
            client.invalidateCache(storeView, new String[0], new String[0]);
            LOGGER.debug("Successfully performed full cache invalidation for store view: {}", storeView);
        } catch (Exception e) {
            LOGGER.error("Error performing full cache invalidation: {}", e.getMessage(), e);
        }
    }

    private Map<String, String[]> getDynamicProperties(ValueMap properties) {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        Set<String> invalidationTypes = invalidateCacheRegistry.getInvalidationTypes();

        for (String invalidationType : invalidationTypes) {
            String[] values = properties.get(invalidationType, String[].class);
            if (values != null && values.length > 0) {
                dynamicProperties.put(invalidationType, values);
            }
        }
        return Collections.unmodifiableMap(dynamicProperties);
    }

    private void invalidateCacheByType(GraphqlClient client, String storeView, Map<String, String[]> dynamicProperties) {
        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            try {
                String[] cachePatterns = getInvalidationPatterns(values, key);
                if (cachePatterns.length > 0) {
                    LOGGER.debug("Invalidating cache for invalidationType: {}", key);
                    client.invalidateCache(storeView, new String[0], cachePatterns);
                } else {
                    LOGGER.debug("No cache patterns generated for invalidationType: {}", key);
                }
            } catch (Exception e) {
                LOGGER.error("Error invalidating cache for invalidationType {}: {}", key, e.getMessage(), e);
            }
        }
    }

    private String[] getInvalidationPatterns(String[] invalidationParameters, String invalidationType) {
        if (invalidationType == null || invalidationParameters == null || invalidationParameters.length == 0) {
            return new String[0];
        }

        InvalidationStrategies strategies = invalidateCacheRegistry.getInvalidationStrategies(invalidationType);
        if (strategies == null) {
            return new String[0];
        }

        return strategies.getStrategies(false).stream()
            .map(strategyInfo -> strategyInfo.getStrategy().getPatterns(invalidationParameters))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .distinct()
            .toArray(String[]::new);
    }
}
