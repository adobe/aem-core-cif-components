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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.cacheinvalidation.internal.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.reflect.TypeToken;

@Component(
    service = InvalidateDispatcherCacheImpl.class,
    immediate = true)
public class InvalidateDispatcherCacheImpl {

    private static final String DISPATCHER_BASE_URL = "http://localhost:80";
    private static final String DISPATCHER_INVALIDATE_PATH = "/dispatcher/invalidate.cache";
    private static final String PATH_DELIMITER = "/";
    private static final String IS_FUNCTION = "isFunction";
    private static final String CQ_ACTION_HEADER = "CQ-Action";
    private static final String CQ_HANDLE_HEADER = "CQ-Handle";
    private static final String CQ_ACTION_SCOPE_HEADER = "CQ-Action-Scope";
    private static final String DELETE_ACTION = "Delete";
    private static final String RESOURCE_ONLY_SCOPE = "ResourceOnly";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @Reference
    private UrlProviderImpl urlProvider;

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    @Reference
    private InvalidateCacheRegistry invalidateCacheRegistry;

    private final HttpClientProvider httpClientProvider = new HttpClientProvider();
    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateDispatcherCacheImpl.class);

    public void invalidateCache(String path) {
        if (path == null || path.trim().isEmpty()) {
            LOGGER.warn("Invalid path provided for cache invalidation");
            return;
        }

        if (!slingSettingsService.getRunModes().contains("author")) {
            LOGGER.error("Operation is only supported for author");
            return;
        }

        try (ResourceResolver resourceResolver = invalidateCacheSupport.getServiceUserResourceResolver()) {
            Resource resource = invalidateCacheSupport.getResource(resourceResolver, path);
            if (resource == null) {
                LOGGER.debug("Resource not found at path: {}", path);
                return;
            }

            Session session = getSession(resourceResolver);
            ValueMap properties = resource.getValueMap();
            String storePath = properties.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);

            ComponentsConfiguration commerceProperties = getCommerceProperties(resourceResolver, storePath);
            if (!isValid(properties, resourceResolver, storePath)) {
                LOGGER.debug("Invalid properties for cache invalidation at path: {}", path);
                return;
            }

            String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, (String) null);
            Map<String, String[]> dynamicProperties = getDynamicProperties(properties);

            GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);
            String[] allPaths = getAllInvalidPaths(session, resourceResolver, client, storePath, dynamicProperties);

            // Remove null or empty values and sort paths
            allPaths = Arrays.stream(allPaths)
                .filter(urlPath -> urlPath != null && !urlPath.trim().isEmpty())
                .sorted(Comparator.comparingInt(urlPath -> urlPath.split(PATH_DELIMITER).length))
                .toArray(String[]::new);

            Set<String> invalidateCachePaths = new HashSet<>();
            for (String urlPath : allPaths) {
                boolean isSubPath = invalidateCachePaths.stream()
                    .anyMatch(topPath -> urlPath.startsWith(topPath + PATH_DELIMITER));
                if (!isSubPath) {
                    invalidateCachePaths.add(urlPath);
                }
            }

            String dispatcherUrl = invalidateCacheSupport.getDispatcherBaseUrl() != null
                ? invalidateCacheSupport.getDispatcherBaseUrl()
                : DISPATCHER_BASE_URL;

            invalidateCachePaths.forEach(invalidatePath -> {
                try {
                    flushCache(invalidatePath, dispatcherUrl);
                } catch (CacheInvalidationException e) {
                    LOGGER.error("Error flushing cache for path {}: {}", path, e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error invalidating cache: {}", e.getMessage(), e);
        }
    }

    protected Map<String, String[]> getDynamicProperties(ValueMap properties) {
        if (properties == null) {
            return Collections.emptyMap();
        }

        Map<String, String[]> dynamicProperties = new HashMap<>();
        Set<String> attributes = invalidateCacheRegistry.getAttributes();

        // Early return if no attributes
        if (attributes.isEmpty()) {
            return dynamicProperties;
        }

        // Process each attribute only once
        for (String attribute : attributes) {
            String[] values = properties.get(attribute, String[].class);
            if (values == null || values.length == 0) {
                continue;
            }

            InvalidateCacheRegistry.AttributeStrategies strategies = invalidateCacheRegistry.getAttributeStrategies(attribute);
            if (strategies == null) {
                continue;
            }

            // Get all strategies at once and filter for DispatcherCacheInvalidationStrategy
            List<InvalidateCacheRegistry.StrategyInfo> dispatcherStrategies = strategies.getStrategies(false).stream()
                .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
                .collect(Collectors.toList());

            // If we found any dispatcher strategies, add the values
            if (!dispatcherStrategies.isEmpty()) {
                dynamicProperties.put(attribute, values);
            }
        }

        return Collections.unmodifiableMap(dynamicProperties);
    }

    protected Session getSession(ResourceResolver resourceResolver) throws CacheInvalidationException {
        if (resourceResolver == null) {
            throw new CacheInvalidationException("ResourceResolver cannot be null");
        }

        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            LOGGER.error("Session not found for resource resolver");
            throw new CacheInvalidationException("Session not found for resource resolver");
        }
        return session;
    }

    protected ComponentsConfiguration getCommerceProperties(ResourceResolver resourceResolver, String storePath) {
        if (resourceResolver == null || storePath == null) {
            return null;
        }
        return invalidateCacheSupport.getCommerceProperties(resourceResolver, storePath);
    }

    protected String[] getAllInvalidPaths(Session session, ResourceResolver resourceResolver, GraphqlClient client,
        String storePath, Map<String, String[]> dynamicProperties) throws CacheInvalidationException {
        if (session == null || resourceResolver == null || client == null || storePath == null || dynamicProperties == null) {
            LOGGER.debug(
                "Invalid parameters for getAllInvalidPaths: session={}, resourceResolver={}, client={}, storePath={}, dynamicProperties={}",
                session != null, resourceResolver != null, client != null, storePath, dynamicProperties != null);
            return EMPTY_STRING_ARRAY;
        }

        Set<String> invalidateDispatcherPagePaths = new HashSet<>();
        Set<String> correspondingPaths = new HashSet<>();

        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            if (values == null || values.length == 0) {
                LOGGER.debug("Skipping empty values for key: {}", key);
                continue;
            }

            try {
                // Get all strategies for this key
                InvalidateCacheRegistry.AttributeStrategies strategies = invalidateCacheRegistry.getAttributeStrategies(key);
                if (strategies == null) {
                    LOGGER.debug("No strategies found for key: {}", key);
                    continue;
                }

                // Process each dispatcher strategy
                List<InvalidateCacheRegistry.StrategyInfo> dispatcherStrategies = strategies.getStrategies(false).stream()
                    .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
                    .collect(Collectors.toList());

                if (dispatcherStrategies.isEmpty()) {
                    LOGGER.debug("No dispatcher strategies found for key: {}", key);
                    continue;
                }

                // Get paths for each strategy
                for (InvalidateCacheRegistry.StrategyInfo info : dispatcherStrategies) {
                    DispatcherCacheInvalidationStrategy strategy = (DispatcherCacheInvalidationStrategy) info.getStrategy();

                    // Get corresponding page paths
                    String[] paths = getCorrespondingPageBasedOnEntries(session, storePath, values, key);
                    if (paths != null && paths.length > 0) {
                        invalidateDispatcherPagePaths.addAll(Arrays.asList(paths));
                    }

                    // Get GraphQL data and invalid paths
                    String query = strategy.getGraphqlQuery(values);
                    if (query != null) {
                        Map<String, Object> data = getGraphqlResponseData(client, query);
                        if (data != null) {
                            String[] invalidPaths = strategy.getPathsToInvalidate(
                                getPage(resourceResolver, storePath),
                                resourceResolver,
                                data,
                                storePath);
                            if (invalidPaths != null && invalidPaths.length > 0) {
                                correspondingPaths.addAll(Arrays.asList(invalidPaths));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error processing invalid paths for key: {} with values: {}", key, Arrays.toString(values), e);
                throw new CacheInvalidationException("Error processing invalid paths for key: " + key, e);
            }
        }

        // Combine and deduplicate paths
        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(invalidateDispatcherPagePaths);
        allPaths.addAll(correspondingPaths);

        LOGGER.debug("Found {} unique paths to invalidate", allPaths.size());
        return allPaths.toArray(new String[0]);
    }

    protected String[] getCorrespondingPageBasedOnEntries(Session session, String storePath, String[] entries, String key)
        throws CacheInvalidationException {
        if (session == null || storePath == null || entries == null || entries.length == 0 || key == null) {
            LOGGER.debug("Invalid parameters for getCorrespondingPageBasedOnEntries: session={}, storePath={}, entries={}, key={}",
                session != null, storePath, entries != null ? entries.length : 0, key);
            return EMPTY_STRING_ARRAY;
        }

        try {
            String entryList = formatList(entries, ", ", "'%s'");
            return invalidateCacheRegistry.getCorrespondingPagePaths(key, session, storePath, entryList);
        } catch (Exception e) {
            LOGGER.error("Error getting corresponding page paths for key: {} with entries: {}", key, Arrays.toString(entries), e);
            throw new CacheInvalidationException("Error getting corresponding page paths", e);
        }
    }

    protected Map<String, Object> getGraphqlResponseData(GraphqlClient client, String query) {
        if (client == null || query == null) {
            LOGGER.debug("Invalid parameters for getGraphqlResponseData: client={}, query={}", client != null, query != null);
            return Collections.emptyMap();
        }

        try {
            GraphqlRequest request = new GraphqlRequest(query);
            Type typeOfT = new TypeToken<Map<String, Object>>() {}.getType();
            Type typeOfU = new TypeToken<Map<String, Object>>() {}.getType();
            GraphqlResponse<Map<String, Object>, Map<String, Object>> response = client.execute(request, typeOfT, typeOfU);

            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                LOGGER.error("GraphQL query errors: {}", response.getErrors());
            }

            return response.getData() != null ? response.getData() : Collections.emptyMap();
        } catch (Exception e) {
            LOGGER.error("Error executing GraphQL query: {}", query, e);
            return Collections.emptyMap();
        }
    }

    protected boolean isValid(ValueMap valueMap, ResourceResolver resourceResolver, String storePath) {
        Map<String, Map<String, Object>> jsonData = createJsonData(resourceResolver, storePath);
        for (Map.Entry<String, Map<String, Object>> entry : jsonData.entrySet()) {
            Map<String, Object> properties = entry.getValue();
            String key = entry.getKey();
            boolean isFunction = (boolean) properties.get(IS_FUNCTION);
            if (isFunction) {
                if (!invokeFunction(properties)) {
                    return false;
                }
            } else {
                if (!checkProperty(valueMap, key, properties)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean invokeFunction(Map<String, Object> properties) {
        String methodName = (String) properties.get("method");
        try {
            Method method;
            Object result;
            Class<?>[] parameterTypes = (Class<?>[]) properties.get("parameterTypes");
            Object[] args = (Object[]) properties.get("args");

            if (parameterTypes != null && args != null) {
                method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod(methodName, parameterTypes);
                result = method.invoke(this, args);
            } else {
                throw new IllegalArgumentException("Invalid method parameters for: " + methodName);
            }
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean checkProperty(ValueMap valueMap, String key, Map<String, Object> properties) {
        boolean isFlag = true;
        Class<?> clazz = (Class<?>) properties.get("class");
        Object value = getPropertiesValue(valueMap, key, clazz);
        if (value instanceof String) {
            isFlag = !((String) value).isEmpty();
        } else if (value instanceof Object[]) {
            isFlag = ((Object[]) value).length != 0;
        }
        return isFlag;
    }

    protected Map<String, Map<String, Object>> createJsonData(ResourceResolver resourceResolver,
        String actualStorePath) {
        Map<String, Map<String, Object>> jsonData = new HashMap<>();

        jsonData.put(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, createProperty(false, String.class));
        jsonData.put(InvalidateCacheSupport.PROPERTIES_STORE_PATH, createProperty(false, String.class));
        jsonData.put("categoryPath", createFunctionProperty("getCorrespondingPageProperties", new Class<?>[] { ResourceResolver.class,
            String.class, String.class },
            new Object[] { resourceResolver, actualStorePath, SiteStructureImpl.PN_CIF_CATEGORY_PAGE }));
        jsonData.put("productPath", createFunctionProperty("getCorrespondingPageProperties", new Class<?>[] { ResourceResolver.class,
            String.class, String.class },
            new Object[] { resourceResolver, actualStorePath, SiteStructureImpl.PN_CIF_PRODUCT_PAGE }));

        return jsonData;
    }

    protected Map<String, Object> createProperty(boolean isFunction, Class<?> clazz) {
        Map<String, Object> property = new HashMap<>();
        property.put(IS_FUNCTION, isFunction);
        property.put("class", clazz);
        return property;
    }

    protected Map<String, Object> createFunctionProperty(String method, Class<?>[] parameterTypes, Object[] args) {
        Map<String, Object> property = new HashMap<>();
        property.put(IS_FUNCTION, true);
        property.put("method", method);
        property.put("parameterTypes", parameterTypes);
        property.put("args", args);
        return property;
    }

    protected <T> T getPropertiesValue(ValueMap properties, String key, Class<T> clazz) {
        return properties.get(key, clazz);
    }

    protected Page getPage(ResourceResolver resourceResolver, String storePath) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager != null) {
            return pageManager.getPage(storePath);
        }
        return null;
    }

    @SuppressWarnings("unused")
    protected String getCorrespondingPageProperties(ResourceResolver resourceResolver, String storePath, String propertyName) {
        Page page = getPage(resourceResolver, storePath);
        if (page != null) {
            ValueMap properties = page.getProperties();
            return properties.get(propertyName, String.class);
        }
        return null;
    }

    protected String formatList(String[] invalidCacheEntries, String delimiter, String pattern) {
        return Arrays.stream(invalidCacheEntries)
            .map(item -> String.format(pattern, item))
            .collect(Collectors.joining(delimiter));
    }

    protected void flushCache(String handle, String dispatcherUrl) throws CacheInvalidationException {
        if (handle == null || handle.trim().isEmpty() || dispatcherUrl == null || dispatcherUrl.trim().isEmpty()) {
            LOGGER.debug("Invalid parameters for flushCache: handle={}, dispatcherUrl={}", handle, dispatcherUrl);
            throw new CacheInvalidationException("Invalid handle or dispatcher URL");
        }

        try (CloseableHttpClient client = httpClientProvider.createHttpClient()) {
            HttpPost post = new HttpPost(dispatcherUrl + DISPATCHER_INVALIDATE_PATH);
            post.setHeader(CQ_ACTION_HEADER, DELETE_ACTION);
            post.setHeader(CQ_HANDLE_HEADER, handle);
            post.setHeader(CQ_ACTION_SCOPE_HEADER, RESOURCE_ONLY_SCOPE);

            try (CloseableHttpResponse response = client.execute(post)) {
                String result = EntityUtils.toString(response.getEntity());
                LOGGER.debug("Cache invalidation result for path {}: {}", handle, result);
            }
        } catch (IOException e) {
            String errorMsg = String.format("IO error while flushing cache for path %s: %s", handle, e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new CacheInvalidationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while flushing cache for path %s: %s", handle, e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new CacheInvalidationException(errorMsg, e);
        }
    }
}
