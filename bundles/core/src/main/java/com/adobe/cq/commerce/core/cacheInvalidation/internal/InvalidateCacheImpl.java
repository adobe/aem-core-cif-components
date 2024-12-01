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

package com.adobe.cq.commerce.core.cacheInvalidation.internal;

import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;

@Component(service = InvalidateCacheImpl.class, immediate = true)
public class InvalidateCacheImpl {

    @Reference
    private ServiceUserService serviceUserService;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateCacheImpl.class);

    public void invalidateCache(String path) {
        try (ResourceResolver resourceResolver = serviceUserService.getServiceUserResourceResolver(InvalidateCacheSupport.SERVICE_USER)) {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                String storePath = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
                Node commerceNode = getCommerceNode(resourceResolver, storePath);
                if (commerceNode != null) {
                    String graphqlClientId = commerceNode.hasProperty(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID) ? commerceNode
                        .getProperty(
                            InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID).getString() : null;
                    String storeView = commerceNode.hasProperty(InvalidateCacheSupport.PROPERTIES_STORE_VIEW) ? commerceNode.getProperty(
                        InvalidateCacheSupport.PROPERTIES_STORE_VIEW)
                        .getString() : "default";

                    // Checks the graphql client exists
                    GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);

                    String[] invalidCacheEntries = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_INVALID_CACHE_ENTRIES,
                        String[].class);
                    String[] listOfCacheToSearch = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_LIST_OF_CACHE_TO_SEARCH,
                        String[].class);

                    String type = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_TYPE, String.class);
                    String attribute = resource.getValueMap().get(InvalidateCacheSupport.PROPERTIES_ATTRIBUTE, String.class);
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
                } else {
                    LOGGER.error("Commerce data not found at path: {}", path);
                }
            } else {
                LOGGER.error("Resource not found at path: {}", path);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getValueFromNodeOrParent(ResourceResolver resourceResolver, String storePath, String propertyName)
        throws RepositoryException {
        Resource pathResource = resourceResolver.getResource(storePath);
        if (pathResource != null && pathResource.adaptTo(Node.class) != null) {
            Node node = pathResource.adaptTo(Node.class);
            while (node != null && !"/content".equals(node.getPath())) {
                Node contentNode = node.hasNode("jcr:content") ? node.getNode("jcr:content") : null;
                if (contentNode != null && contentNode.hasProperty(propertyName)) {
                    return contentNode.getProperty(propertyName).getString();
                }
                node = node.getParent();
            }
        }

        return null; // or throw an exception if value is not found
    }

    private static Node getCommerceDataNode(ResourceResolver resourceResolver, String path)
        throws RepositoryException {
        String specificPath = path + "/settings/cloudconfigs/commerce/jcr:content";
        Resource pathResource = resourceResolver.getResource(specificPath);
        if (pathResource != null && pathResource.adaptTo(Node.class) != null) {
            return pathResource.adaptTo(Node.class);
        }
        return null;
    }

    private static Node getCommerceNode(ResourceResolver resourceResolver, String storePath) {
        try {
            String commerceConfigPath = getValueFromNodeOrParent(resourceResolver, storePath, "cq:conf");
            if (commerceConfigPath != null) {
                return getCommerceDataNode(resourceResolver, commerceConfigPath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static String getRegexBasedOnAttribute(String attribute) {
        String regex;
        switch (attribute) {
            case "uuid":
                regex = "\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
                break;
            default:
                regex = "\"" + attribute + "\":\\s*\"";
        }
        return regex;
    }

    private static String[] getAttributePatterns(String[] patterns, String attribute) {
        String attributeString = String.join("|", patterns);
        return new String[] { getRegexBasedOnAttribute(attribute) + "(" + attributeString + ")\"" };
    }
}
