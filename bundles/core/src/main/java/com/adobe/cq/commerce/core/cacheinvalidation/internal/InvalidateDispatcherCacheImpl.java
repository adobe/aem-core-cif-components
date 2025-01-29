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
import java.util.stream.Stream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.*;
import javax.jcr.query.Query;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.cacheinvalidation.services.InvalidateCache;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.reflect.TypeToken;

@Component(
    service = InvalidateDispatcherCacheImpl.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class InvalidateDispatcherCacheImpl {

    private static final String HTML_SUFFIX = ".html";

    @Reference
    private UrlProviderImpl urlProvider;

    String pathDelimiter = "/";

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateDispatcherCacheImpl.class);

    private static final String IS_FUNCTION = "isFunction";

    @Reference
    private InvalidateCacheRegistry invalidateCacheRegistry;

    public static class CacheInvalidationException extends Exception {
        public CacheInvalidationException(String message) {
            super(message);
        }

        public CacheInvalidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void invalidateCache(String path) {
        // To Do: Change this to for non-author run modes
        if (slingSettingsService.getRunModes().contains("author")) {
            LOGGER.error("Operation is only supported for author");
            return;
        }
        try (ResourceResolver resourceResolver = invalidateCacheSupport.getServiceUserResourceResolver()) {
            Resource resource = invalidateCacheSupport.getResource(resourceResolver, path);
            if (resource == null) {
                return;
            }
            Session session = getSession(resourceResolver);

            ValueMap properties = resource.getValueMap();
            String storePath = properties.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
            ComponentsConfiguration commerceProperties = getCommerceProperties(resourceResolver, storePath);
            if (!isValid(properties, resourceResolver, storePath))
                return;

            String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, (String) null);
            // Store dynamic properties in a map
            Map<String, String[]> dynamicProperties = getDynamicProperties(properties);

            GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);

            String[] allPaths = getAllInvalidPaths(session, resourceResolver, client, storePath, dynamicProperties);
            // Remove null or empty values
            allPaths = Arrays.stream(allPaths).filter(urlPath -> urlPath != null && !urlPath.isEmpty()).toArray(String[]::new);

            // Sort paths based on the number of '/' characters in increasing order
            Arrays.sort(allPaths, Comparator.comparingInt(urlPath -> urlPath.split("/").length));

            Set<String> invalidateCachePaths = new HashSet<>();
            for (String urlPath : allPaths) {
                boolean isSubPath = invalidateCachePaths.stream().anyMatch(topPath -> urlPath.startsWith(topPath + pathDelimiter));
                if (!isSubPath) {
                    invalidateCachePaths.add(urlPath);
                }
            }

            invalidateCachePaths.forEach(invalidatePath -> {
                try {
                    flushCache(invalidatePath);
                } catch (CacheInvalidationException e) {
                    LOGGER.error("Error flushing cache for path {}: {}", path, e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error invalidating cache: {}", e.getMessage(), e);
        }
    }

    private Map<String, String[]> getDynamicProperties(ValueMap properties) {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        for (String attribute : invalidateCacheRegistry.getAttributes()) {
            InvalidateCache invalidateCache = invalidateCacheRegistry.get(attribute);
            if (invalidateCache != null && invalidateCache.canDoDispatcherCacheInvalidation()) {
                String[] values = properties.get(attribute, String[].class);
                if (values != null) {
                    dynamicProperties.put(attribute, values);
                }
            }
        }
        return dynamicProperties;
    }

    private Session getSession(ResourceResolver resourceResolver) throws Exception {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            LOGGER.error("Session not found for resource resolver");
            throw new Exception("Session not found for resource resolver");
        }
        return session;
    }

    private ComponentsConfiguration getCommerceProperties(ResourceResolver resourceResolver, String storePath) {
        return invalidateCacheSupport.getCommerceProperties(resourceResolver, storePath);
    }

    private String[] getAllInvalidPaths(Session session, ResourceResolver resourceResolver, GraphqlClient client,
        String storePath, Map<String, String[]> dynamicProperties) throws CacheInvalidationException {
        String[] invalidateDispatcherPagePaths = new String[0];
        String[] correspondingPaths = new String[0];

        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            if (values != null && values.length > 0) {
                try {
                    String[] paths = getCorrespondingPageBasedOnEntries(session, storePath, values, key);
                    String query = invalidateCacheRegistry.getGraphqlQuery(key, values);
                    Map<String, Object> data = getGraphqlResponseData(client, query);
                    if (data != null) {
                        String[] invalidPaths = getInvalidPaths(resourceResolver, data, storePath, key);
                        correspondingPaths = Stream.concat(Arrays.stream(correspondingPaths), Arrays.stream(invalidPaths))
                            .toArray(String[]::new);
                    }
                    invalidateDispatcherPagePaths = Stream.concat(Arrays.stream(invalidateDispatcherPagePaths), Arrays.stream(paths))
                        .toArray(String[]::new);
                } catch (Exception e) {
                    throw new CacheInvalidationException("Error getting invalid paths for key: " + key, e);
                }
            }
        }

        return Stream.concat(Arrays.stream(invalidateDispatcherPagePaths), Arrays.stream(correspondingPaths))
            .toArray(String[]::new);
    }

    private String[] getCorrespondingPageBasedOnEntries(Session session, String storePath, String[] entries, String key)
        throws CacheInvalidationException {
        String entryList = formatList(entries, ", ", "'%s'");
        try {
            String sqlQuery = invalidateCacheRegistry.getQuery(key, storePath, entryList);
            if (sqlQuery != null) {
                return getQueryResult(getSqlQuery(session, sqlQuery));
            }
        } catch (Exception e) {
            throw new CacheInvalidationException("Error getting corresponding page based on entries", e);
        }
        return new String[0];
    }

    private String[] getInvalidPaths(ResourceResolver resourceResolver, Map<String, Object> data,
        String storePath, String key) {
        Page page = getPage(resourceResolver, storePath);
        return invalidateCacheRegistry.getInvalidPaths(key, page, resourceResolver, data, storePath);
    }

    private static Map<String, Object> getGraphqlResponseData(GraphqlClient client, String query) {
        GraphqlRequest request = new GraphqlRequest(query);
        Type typeOfT = new TypeToken<Map<String, Object>>() {}.getType();
        Type typeOfU = new TypeToken<Map<String, Object>>() {}.getType();
        GraphqlResponse<Map<String, Object>, Map<String, Object>> response = client.execute(request, typeOfT, typeOfU);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            LOGGER.error("Error executing GraphQL query: {}", response.getErrors());
        } else {
            return response.getData();
        }
        return response.getData() != null ? response.getData() : Collections.emptyMap();

    }

    private static boolean isValid(ValueMap valueMap, ResourceResolver resourceResolver, String storePath) {
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

    private static boolean invokeFunction(Map<String, Object> properties) {
        String methodName = (String) properties.get("method");
        try {
            Method method;
            Object result;
            Class<?>[] parameterTypes = (Class<?>[]) properties.get("parameterTypes");
            Object[] args = (Object[]) properties.get("args");

            if (parameterTypes != null && args != null) {
                method = InvalidateDispatcherCacheImpl.class.getDeclaredMethod(methodName, parameterTypes);
                result = method.invoke(null, args);
            } else {
                throw new IllegalArgumentException("Invalid method parameters for: " + methodName);
            }
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkProperty(ValueMap valueMap, String key, Map<String, Object> properties) {
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

    private static Map<String, Map<String, Object>> createJsonData(ResourceResolver resourceResolver,
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

    private static Map<String, Object> createProperty(boolean isFunction, Class<?> clazz) {
        Map<String, Object> property = new HashMap<>();
        property.put(IS_FUNCTION, isFunction);
        property.put("class", clazz);
        return property;
    }

    private static Map<String, Object> createFunctionProperty(String method, Class<?>[] parameterTypes, Object[] args) {
        Map<String, Object> property = new HashMap<>();
        property.put(IS_FUNCTION, true);
        property.put("method", method);
        property.put("parameterTypes", parameterTypes);
        property.put("args", args);
        return property;
    }

    private static <T> T getPropertiesValue(ValueMap properties, String key, Class<T> clazz) {
        return properties.get(key, clazz);
    }

    private static Page getPage(ResourceResolver resourceResolver, String storePath) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager != null) {
            return pageManager.getPage(storePath);
        }
        return null;
    }

    private static String getCorrespondingPageProperties(ResourceResolver resourceResolver, String storePath, String propertyName)
        throws RepositoryException {
        Page page = getPage(resourceResolver, storePath);
        if (page != null) {
            ValueMap properties = page.getProperties();
            return properties.get(propertyName, String.class);
        }
        return null;
    }

    private static String formatList(String[] invalidCacheEntries, String delimiter, String pattern) {
        return Arrays.stream(invalidCacheEntries)
            .map(item -> String.format(pattern, item))
            .collect(Collectors.joining(delimiter));
    }

    private Query getSqlQuery(Session session, String sql2Query) throws CacheInvalidationException {
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            return queryManager.createQuery(sql2Query, Query.JCR_SQL2);
        } catch (Exception e) {
            throw new CacheInvalidationException("Error creating SKU-based SQL2 query", e);
        }
    }

    private String[] getQueryResult(Query query) throws CacheInvalidationException {
        try {
            Set<String> uniquePagePaths = new HashSet<>();

            QueryResult result = query.execute();
            if (result != null) {
                RowIterator rows = result.getRows();
                while (rows.hasNext()) {
                    Row row = rows.nextRow();
                    String fullPath = row.getPath("content");
                    if (fullPath != null) {
                        String pagePath = extractPagePath(fullPath) + HTML_SUFFIX;
                        uniquePagePaths.add(pagePath);
                    }
                }
            }
            return uniquePagePaths.toArray(new String[0]);
        } catch (Exception e) {
            throw new CacheInvalidationException("Error getting query result", e);
        }
    }

    private String extractPagePath(String fullPath) {
        int jcrContentIndex = fullPath.indexOf("/jcr:content");
        return jcrContentIndex != -1 ? fullPath.substring(0, jcrContentIndex) : fullPath;
    }

    private void flushCache(String handle) throws CacheInvalidationException {
        String server = "localhost:80";
        String uri = "/dispatcher/invalidate.cache";
        String url = "http://" + server + uri;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("CQ-Action", "Delete");
            post.setHeader("CQ-Handle", handle);
            post.setHeader("CQ-Action-Scope", "ResourceOnly");

            try (CloseableHttpResponse response = client.execute(post)) {
                String result = EntityUtils.toString(response.getEntity());
                LOGGER.info("result: {}", result);
            }
        } catch (IOException e) {
            throw new CacheInvalidationException("IO error", e);
        } catch (Exception e) {
            LOGGER.error("Flushcache servlet exception: {}", e.getMessage());
        }
    }
}
