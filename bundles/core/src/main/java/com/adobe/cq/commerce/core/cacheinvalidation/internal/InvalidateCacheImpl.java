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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.Objects;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
        try (ResourceResolver resourceResolver = invalidateCacheSupport.getResourceResolver()) {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                processResource(resourceResolver, resource);
            } else {
                LOGGER.error("Resource not found at path: {}", path);
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
            LOGGER.error("Commerce data not found at path: {}", resource.getPath());
        }
    }

    private void handleCacheInvalidation(Resource resource, ComponentsConfiguration commerceProperties) {
        String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, String.class);
        String storeView = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_STORE_VIEW, "default");

        GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);

        String[] invalidCacheEntries = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_INVALID_CACHE_ENTRIES, String[].class);
        String[] listOfCacheToSearch = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_LIST_OF_CACHE_TO_SEARCH,
            String[].class);

        String type = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_TYPE, String.class);
        String attribute = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_ATTRIBUTE, String.class);

        invalidateCacheByType(client, storeView, listOfCacheToSearch, invalidCacheEntries, type, attribute);
    }

    private void invalidateCacheByType(GraphqlClient client, String storeView, String[] listOfCacheToSearch, String[] invalidCacheEntries,
        String type, String attribute) {
        String[] cachePatterns;
        switch (Objects.requireNonNull(type)) {
            case InvalidateCacheSupport.TYPE_SKU:
                cachePatterns = getAttributePatterns(invalidCacheEntries, "sku");
                client.invalidateCache(storeView, listOfCacheToSearch, cachePatterns);
                break;
            case InvalidateCacheSupport.TYPE_CATEGORY:
            case InvalidateCacheSupport.TYPE_UUIDS:
                cachePatterns = getAttributePatterns(invalidCacheEntries, "uuid");
                client.invalidateCache(storeView, listOfCacheToSearch, cachePatterns);
                break;
            case InvalidateCacheSupport.TYPE_ATTRIBUTE:
                cachePatterns = getAttributePatterns(invalidCacheEntries, attribute);
                client.invalidateCache(storeView, listOfCacheToSearch, cachePatterns);
                break;
            case InvalidateCacheSupport.TYPE_ClEAR_SPECIFIC_CACHE:
                client.invalidateCache(storeView, invalidCacheEntries, null);
                break;
            case InvalidateCacheSupport.TYPE_CLEAR_ALL:
                client.invalidateCache(storeView, null, null);
                break;
            default:
                LOGGER.warn("Unknown cache type: {}", type);
                throw new IllegalStateException("Unknown cache type" + type);
        }
    }

    private static String getRegexBasedOnAttribute(String attribute) {
        switch (attribute) {
            case "uuid":
                return "\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
            default:
                return "\"" + attribute + "\":\\s*\"";
        }
    }

    private static String[] getAttributePatterns(String[] patterns, String attribute) {
        String attributeString = String.join("|", patterns);
        return new String[] { getRegexBasedOnAttribute(attribute) + "(" + attributeString + ")\"" };
    }
}
