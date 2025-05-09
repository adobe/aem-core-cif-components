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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
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
    private InvalidateCacheSupport invalidateCacheSupport;

    @Reference
    private InvalidateCacheRegistry invalidateCacheRegistry;

    private final HttpClientProvider httpClientProvider = new HttpClientProvider();
    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateDispatcherCacheImpl.class);

    public void invalidateCache(String path) {
        try (ResourceResolver resourceResolver = invalidateCacheSupport.getServiceUserResourceResolver()) {
            Resource resource = invalidateCacheSupport.getResource(resourceResolver, path);
            if (resource == null) {
                LOGGER.debug("Resource not found at path: {}", path);
                return;
            }

            ValueMap properties = resource.getValueMap();
            String storePath = properties.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
            String dispatcherUrl = Optional.ofNullable(invalidateCacheSupport.getDispatcherBaseUrl())
                .orElse(DISPATCHER_BASE_URL);
            String dispatcherBasePath = invalidateCacheSupport.getDispatcherBasePathForStorePath(storePath);

            if (shouldPerformFullCacheClear(properties)) {
                flushCacheForPaths(Collections.singletonList(dispatcherBasePath), dispatcherUrl, path);
                return;
            }

            // Check if properties are invalid
            if (!isValid(properties, resourceResolver, storePath)) {
                LOGGER.debug("Required properties are not been set for the storepath {}", storePath);
                return;
            }

            Resource commerceResource = invalidateCacheSupport.getResource(resourceResolver, storePath);
            MagentoGraphqlClient client = commerceResource.adaptTo(MagentoGraphqlClient.class);
            if (client == null) {
                LOGGER.debug("Magento client not found for store path: {}", storePath);
                return;
            }

            Map<String, String[]> dynamicProperties = getDynamicProperties(properties);
            List<String> processedPaths = processAndConvertPaths(
                getAllInvalidPaths(resourceResolver, client, storePath, dynamicProperties));
            flushCacheForPaths(processedPaths, dispatcherUrl, path);

        } catch (Exception e) {
            LOGGER.error("Error invalidating cache for path {}: {}", path, e.getMessage(), e);
        }
    }

    private boolean shouldPerformFullCacheClear(ValueMap properties) {
        // Check for invalidateAll flag

        if (properties.get(InvalidateCacheSupport.PROPERTIES_INVALIDATE_ALL, false)) {
            LOGGER.debug("PROPERTIES_INVALIDATE_ALL is true");
            return true;
        }
        return false;
    }

    private List<String> processAndConvertPaths(List<String> paths) {
        return paths.stream()
            .filter(Objects::nonNull)
            .filter(path -> !path.trim().isEmpty())
            .map(invalidateCacheSupport::convertUrlPath)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(path -> path.split(PATH_DELIMITER).length))
            .reduce(new ArrayList<>(), (result, currentPath) -> {
                boolean isRedundant = result.stream()
                    .anyMatch(existingPath -> currentPath.startsWith(existingPath + PATH_DELIMITER));
                if (!isRedundant) {
                    result.add(currentPath);
                }
                return result;
            }, (list1, list2) -> list1);
    }

    private void flushCacheForPaths(List<String> paths, String dispatcherUrl, String originalPath) {
        paths.forEach(invalidatePath -> {
            try {
                flushCache(invalidatePath, dispatcherUrl);
            } catch (CacheInvalidationException e) {
                LOGGER.error("Error flushing cache for path {}: {}", originalPath, e.getMessage());
            }
        });
    }

    protected Map<String, String[]> getDynamicProperties(ValueMap properties) {
        return invalidateCacheRegistry.getInvalidationTypes().stream()
            .filter(invalidationType -> {
                String[] values = properties.get(invalidationType, String[].class);
                InvalidationStrategies strategies = invalidateCacheRegistry.getInvalidationStrategies(invalidationType);
                return values != null && values.length > 0 && strategies != null &&
                    strategies.getStrategies(false).stream()
                        .anyMatch(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy);
            })
            .collect(Collectors.toMap(
                Function.identity(),
                invalidationType -> properties.get(invalidationType, String[].class),
                (existing, replacement) -> existing,
                HashMap::new));
    }

    protected List<String> getAllInvalidPaths(ResourceResolver resourceResolver, MagentoGraphqlClient client,
        String storePath, Map<String, String[]> dynamicProperties) throws CacheInvalidationException {
        Set<String> allPaths = new HashSet<>();
        String dispatcherBasePath = invalidateCacheSupport.getDispatcherBasePathForStorePath(storePath);
        Page page = getPage(resourceResolver, storePath);

        // Process each invalidationType strategy
        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            if (!isValidEntry(entry)) {
                continue;
            }

            try {
                Set<String> paths = processInvalidationStrategy(entry, page, resourceResolver, storePath, client);
                if (paths.contains(dispatcherBasePath)) {
                    LOGGER.debug("Found base path in invalidation paths, performing full cache clear");
                    return Collections.singletonList(dispatcherBasePath);
                }
                allPaths.addAll(paths);
            } catch (Exception e) {
                LOGGER.error("Error processing strategy for key: {}", entry.getKey(), e);
            }
        }

        LOGGER.debug("Found {} unique paths to invalidate", allPaths.size());
        return new ArrayList<>(allPaths);
    }

    private Set<String> processInvalidationStrategy(Map.Entry<String, String[]> entry, Page page,
        ResourceResolver resourceResolver, String storePath, MagentoGraphqlClient client) {
        Set<String> paths = new HashSet<>();
        InvalidationStrategies strategies = invalidateCacheRegistry.getInvalidationStrategies(entry.getKey());

        strategies.getStrategies(false).stream()
            .filter(info -> info.getStrategy() instanceof DispatcherCacheInvalidationStrategy)
            .forEach(info -> {
                try {
                    DispatcherCacheInvalidationStrategy strategy = (DispatcherCacheInvalidationStrategy) info.getStrategy();
                    List<String> invalidationParameters = new ArrayList<>(Arrays.asList(entry.getValue()));

                    CacheInvalidationContext context = new CacheInvalidationContextImpl(
                        page,
                        resourceResolver,
                        invalidationParameters,
                        storePath,
                        client);

                    List<String> invalidPaths = strategy.getPathsToInvalidate(context);
                    if (invalidPaths != null) {
                        paths.addAll(invalidPaths.stream()
                            .filter(Objects::nonNull)
                            .filter(path -> !path.trim().isEmpty())
                            .collect(Collectors.toSet()));
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception was thrown by {}: ", info.getStrategy().getClass(), e);
                }
            });

        return paths;
    }

    private boolean isValidEntry(Map.Entry<String, String[]> entry) {
        if (entry == null || entry.getValue() == null || entry.getValue().length == 0) {
            LOGGER.debug("Invalid entry: {} entry", entry == null ? "null" : "empty values for key: " + entry.getKey());
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

    protected void flushCache(String handle, String dispatcherUrl) throws CacheInvalidationException {
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
}
