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

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

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

            String[] allPaths = getAllInvalidPaths(resourceResolver, client, storePath, dynamicProperties);
            allPaths = processAndConvertPaths(allPaths);

            String dispatcherUrl = Optional.ofNullable(invalidateCacheSupport.getDispatcherBaseUrl())
                .orElse(DISPATCHER_BASE_URL);

            flushCacheForPaths(allPaths, dispatcherUrl, path);

        } catch (Exception e) {
            LOGGER.error("Error invalidating cache: {}", e.getMessage(), e);
        }
    }

    private String[] processAndConvertPaths(String[] paths) {
        return Arrays.stream(paths)
            .filter(Objects::nonNull)
            .filter(path -> !path.trim().isEmpty())
            .map(urlPath -> invalidateCacheSupport.convertUrlPath(urlPath))
            .sorted(Comparator.comparingInt(path -> path.split(PATH_DELIMITER).length))
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                filteredPaths -> {
                    List<String> result = new ArrayList<>();
                    for (String currentPath : filteredPaths) {
                        if (result.stream().noneMatch(existingPath -> currentPath.startsWith(existingPath + PATH_DELIMITER))) {
                            result.add(currentPath);
                        }
                    }
                    return result.toArray(new String[0]);
                }));
    }

    private void flushCacheForPaths(String[] paths, String dispatcherUrl, String originalPath) {
        Arrays.stream(paths).forEach(invalidatePath -> {
            try {
                flushCache(invalidatePath, dispatcherUrl);
            } catch (CacheInvalidationException e) {
                LOGGER.error("Error flushing cache for path {}: {}", originalPath, e.getMessage());
            }
        });
    }

    protected Map<String, String[]> getDynamicProperties(ValueMap properties) {
        if (properties == null) {
            return Collections.emptyMap();
        }

        Map<String, String[]> dynamicProperties = new HashMap<>();
        Set<String> attributes = invalidateCacheRegistry.getAttributes();

        if (attributes.isEmpty()) {
            return dynamicProperties;
        }

        attributes.stream()
            .filter(attribute -> {
                String[] values = properties.get(attribute, String[].class);
                AttributeStrategies strategies = invalidateCacheRegistry.getAttributeStrategies(attribute);
                return values != null && values.length > 0 && strategies != null &&
                    strategies.getStrategies(false).stream()
                        .anyMatch(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy);
            })
            .forEach(attribute -> dynamicProperties.put(attribute, properties.get(attribute, String[].class)));

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

    private boolean validateParameters(ResourceResolver resourceResolver, GraphqlClient client,
        String storePath, Map<String, String[]> dynamicProperties) {
        if (resourceResolver == null || client == null || storePath == null || dynamicProperties == null) {
            LOGGER.debug(
                "Invalid parameters for getAllInvalidPaths: resourceResolver={}, client={}, storePath={}, dynamicProperties={}",
                resourceResolver != null, client != null, storePath, dynamicProperties != null);
            return false;
        }
        return true;
    }

    protected String[] getAllInvalidPaths(ResourceResolver resourceResolver, GraphqlClient client,
        String storePath, Map<String, String[]> dynamicProperties) throws CacheInvalidationException {
        if (!validateParameters(resourceResolver, client, storePath, dynamicProperties)) {
            return new String[0];
        }

        Set<String> allPaths = processInvalidPaths(resourceResolver, client, storePath, dynamicProperties);
        LOGGER.debug("Found {} unique paths to invalidate", allPaths.size());
        return allPaths.toArray(new String[0]);
    }

    private Set<String> processInvalidPaths(ResourceResolver resourceResolver, GraphqlClient client,
        String storePath, Map<String, String[]> dynamicProperties) throws CacheInvalidationException {

        Set<String> allPaths = new HashSet<>();
        String dispatcherBasePath = invalidateCacheSupport.getDispatcherBasePathForStorePath(storePath);

        // Check for full cache clear conditions
        if (dynamicProperties.isEmpty()) {
            LOGGER.debug("Empty dynamic properties, performing full cache clear for path: {}", dispatcherBasePath);
            allPaths.add(dispatcherBasePath);
            return allPaths;
        }

        Set<String> correspondingPaths = new HashSet<>();
        Page page = getPage(resourceResolver, storePath);

        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            if (!isValidEntry(entry)) {
                continue;
            }

            try {
                processAttributeStrategies(entry, page, resourceResolver, storePath, client, correspondingPaths);
            } catch (Exception e) {
                LOGGER.error("Error processing strategies for key: {}", entry.getKey(), e);
            }
        }

        if (correspondingPaths.contains(dispatcherBasePath)) {
            LOGGER.debug("Corresponding paths contain base path, performing full cache clear for path: {}", dispatcherBasePath);
            allPaths.add(dispatcherBasePath);
            return allPaths;
        }

        allPaths.addAll(correspondingPaths);
        LOGGER.debug("Processed {} paths for invalidation", allPaths.size());
        return allPaths;
    }

    private void processAttributeStrategies(Map.Entry<String, String[]> entry, Page page,
        ResourceResolver resourceResolver, String storePath, GraphqlClient client, Set<String> correspondingPaths) {

        AttributeStrategies strategies = invalidateCacheRegistry.getAttributeStrategies(entry.getKey());
        List<StrategyInfo> dispatcherStrategies = getDispatcherStrategies(strategies);

        for (StrategyInfo info : dispatcherStrategies) {
            try {
                DispatcherCacheInvalidationStrategy strategy = (DispatcherCacheInvalidationStrategy) info.getStrategy();
                DispatcherCacheInvalidationContext context = new DispatcherCacheInvalidationContext(
                    page,
                    resourceResolver,
                    entry,
                    storePath,
                    client);

                String[] invalidPaths = strategy.getPathsToInvalidate(context);

                if (invalidPaths != null && invalidPaths.length > 0) {
                    Arrays.stream(invalidPaths)
                        .filter(Objects::nonNull)
                        .filter(path -> !path.trim().isEmpty())
                        .forEach(correspondingPaths::add);
                }
            } catch (Exception e) {
                LOGGER.error("Error processing GraphQL strategy for key: {}", entry.getKey(), e);
            }
        }
    }

    private boolean isValidEntry(Map.Entry<String, String[]> entry) {
        if (entry == null) {
            LOGGER.debug("Invalid entry: null entry");
            return false;
        }

        String key = entry.getKey();
        String[] values = entry.getValue();

        if (values == null || values.length == 0) {
            LOGGER.debug("Invalid entry: empty values for key: {}", key);
            return false;
        }

        return true;
    }

    protected boolean isValid(ValueMap valueMap, ResourceResolver resourceResolver, String storePath) {
        Map<String, Map<String, Object>> jsonData = createJsonData(resourceResolver, storePath);
        return jsonData.entrySet().stream()
            .allMatch(entry -> {
                Map<String, Object> properties = entry.getValue();
                String key = entry.getKey();
                boolean isFunction = (boolean) properties.get(IS_FUNCTION);
                return isFunction ? invokeFunction(properties) : checkProperty(valueMap, key, properties);
            });
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
                return result != null;
            }
            LOGGER.error("Invalid method parameters for method: {}", methodName);
            return false;
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to invoke method {}: {}", methodName, e.getMessage(), e);
            return false;
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error invoking method {}: {}", methodName, e.getMessage(), e);
            return false;
        }
    }

    protected boolean checkProperty(ValueMap valueMap, String key, Map<String, Object> properties) {
        Class<?> clazz = (Class<?>) properties.get("class");
        Object value = getPropertiesValue(valueMap, key, clazz);
        if (value instanceof String) {
            return !((String) value).isEmpty();
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length != 0;
        }
        return true;
    }

    protected Map<String, Map<String, Object>> createJsonData(ResourceResolver resourceResolver,
        String actualStorePath) {
        Map<String, Map<String, Object>> jsonData = new HashMap<>();

        jsonData.put(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, createProperty(false, String.class));
        jsonData.put(InvalidateCacheSupport.PROPERTIES_STORE_PATH, createProperty(false, String.class));
        jsonData.put("categoryPath", createFunctionProperty("getCorrespondingPageProperties",
            new Class<?>[] { ResourceResolver.class, String.class, String.class },
            new Object[] { resourceResolver, actualStorePath, SiteStructureImpl.PN_CIF_CATEGORY_PAGE }));
        jsonData.put("productPath", createFunctionProperty("getCorrespondingPageProperties",
            new Class<?>[] { ResourceResolver.class, String.class, String.class },
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
        return pageManager != null ? pageManager.getPage(storePath) : null;
    }

    @SuppressWarnings("unused")
    protected String getCorrespondingPageProperties(ResourceResolver resourceResolver, String storePath, String propertyName) {
        Page page = getPage(resourceResolver, storePath);
        return page != null ? page.getProperties().get(propertyName, String.class) : null;
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
            throw new CacheInvalidationException(
                String.format("IO error while flushing cache for path %s", handle), e);
        } catch (Exception e) {
            throw new CacheInvalidationException(
                String.format("Unexpected error while flushing cache for path %s", handle), e);
        }
    }

    private List<StrategyInfo> getDispatcherStrategies(AttributeStrategies strategies) {
        return strategies.getStrategies(false).stream()
            .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
            .collect(Collectors.toList());
    }
}
