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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        Map<String, String[]> dynamicProperties = getDynamicProperties(properties);
        if (dynamicProperties.isEmpty()) {
            LOGGER.debug("No dynamic properties found for cache invalidation");
        }

        String storeView = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_STORE_VIEW, DEFAULT_STORE_VIEW);
        String[] listOfCacheToSearch = properties.get(InvalidateCacheSupport.PROPERTIES_CACHE_NAME, String[].class);
        invalidateCacheByType(client, storeView, listOfCacheToSearch, dynamicProperties);
    }

    private Map<String, String[]> getDynamicProperties(ValueMap properties) {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        Set<String> attributes = invalidateCacheRegistry.getAttributes();

        for (String attribute : attributes) {
            String[] values = properties.get(attribute, String[].class);
            if (values != null && values.length > 0) {
                dynamicProperties.put(attribute, values);
            }
        }
        return Collections.unmodifiableMap(dynamicProperties);
    }

    private void invalidateCacheByType(GraphqlClient client, String storeView, String[] listOfCacheToSearch,
        Map<String, String[]> dynamicProperties) {
        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            try {
                String[] cachePatterns = getAttributePatterns(values, key);
                if (cachePatterns.length > 0) {
                    client.invalidateCache(storeView, listOfCacheToSearch, cachePatterns);
                }
            } catch (Exception e) {
                LOGGER.error("Error invalidating cache for attribute {}: {}", key, e.getMessage(), e);
            }
        }
    }

    private String[] getAttributePatterns(String[] patterns, String attribute) {
        if (attribute == null || patterns == null || patterns.length == 0) {
            return new String[0];
        }

        Set<String> resultPatterns = new HashSet<>();
        AttributeStrategies strategies = invalidateCacheRegistry.getAttributeStrategies(attribute);

        if (strategies != null) {
            for (StrategyInfo strategyInfo : strategies.getStrategies(false)) {
                String pattern = strategyInfo.getStrategy().getPattern();
                if (pattern != null) {
                    String attributeString = String.join("|", patterns);
                    resultPatterns.add(pattern + "(" + attributeString + ")");
                } else if ("regexPatterns".equals(attribute)) {
                    resultPatterns.addAll(Arrays.asList(patterns));
                }
            }
        }
        return resultPatterns.toArray(new String[0]);
    }
}
