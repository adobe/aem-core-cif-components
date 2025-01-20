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

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateCacheImpl.class);

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
        String storeView = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_STORE_VIEW, "default");

        GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);
        ValueMap properties = resource.getValueMap();

        String[] listOfCacheToSearch = properties.get(InvalidateCacheSupport.PROPERTIES_CACHE_NAME,
            String[].class);
        // Store dynamic properties in a map
        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("productSkus", properties.get(InvalidateCacheSupport.PROPERTIES_PRODUCT_SKUS, String[].class));
        dynamicProperties.put("categoryUids", properties.get(InvalidateCacheSupport.PROPERTIES_CATEGORY_UIDS, String[].class));
        dynamicProperties.put("regexPatterns", properties.get(InvalidateCacheSupport.PROPERTIES_REGEX_PATTERNS, String[].class));

        invalidateCacheByType(client, storeView, listOfCacheToSearch, dynamicProperties);
    }

    private void invalidateCacheByType(GraphqlClient client, String storeView, String[] listOfCacheToSearch,
        Map<String, String[]> dynamicProperties) {
        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            if (values != null && values.length > 0) {
                String[] cachePatterns;
                if ("regexPatterns".equals(key)) {
                    cachePatterns = values;
                } else {
                    cachePatterns = getAttributePatterns(values, key);
                }
                client.invalidateCache(storeView, listOfCacheToSearch, cachePatterns);
            }
        }
    }

    private static String getRegexBasedOnAttribute(String attribute) {
        switch (attribute) {
            case "categoryUids":
                return "\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
            case "productSkus":
                return "\"sku\":\\s*\"";
            default:
                return "\"" + attribute + "\":\\s*\"";
        }
    }

    private static String[] getAttributePatterns(String[] patterns, String attribute) {
        String attributeString = String.join("|", patterns);
        return new String[] { getRegexBasedOnAttribute(attribute) + "(" + attributeString + ")\"" };
    }
}
