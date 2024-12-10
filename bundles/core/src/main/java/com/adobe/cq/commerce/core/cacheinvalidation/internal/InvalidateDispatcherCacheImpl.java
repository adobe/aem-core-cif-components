/*******************************************************************************
 *
 *    Copyright 2024 Adobe. All rights reserved.
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
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.reflect.TypeToken;

@Component(service = InvalidateDispatcherCacheImpl.class, immediate = true)
public class InvalidateDispatcherCacheImpl {

    static final String PROPERTY_PRODUCT_PAGE_URL_FORMAT = "productPageUrlFormat";
    static final String PROPERTY_CATEGORY_PAGE_URL_FORMAT = "categoryPageUrlFormat";

    @Reference
    private UrlProviderImpl urlProvider;

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ServiceUserService serviceUserService;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateDispatcherCacheImpl.class);

    public void invalidateCache(String path) {
        if (!slingSettingsService.getRunModes().contains("author")) {
            LOGGER.error("Operation is only supported for author");
            return;
        }
        try (ResourceResolver resourceResolver = serviceUserService.getServiceUserResourceResolver(InvalidateCacheSupport.SERVICE_USER)) {
            Resource resource = resourceResolver.getResource(path);
            if (resource == null) {
                LOGGER.error("Resource not found at path: {}", path);
                return;
            }

            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                LOGGER.error("Session not found for resource resolver");
                return;
            }

            ValueMap properties = resource.getValueMap();
            String storePath = properties.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
            ComponentsConfiguration commerceProperties = InvalidateCacheSupport.getCommerceProperties(resourceResolver, storePath);
            if (commerceProperties == null || !isValid(properties, resourceResolver, commerceProperties, storePath)) {
                LOGGER.error("Commerce data not found or invalid at path: {}", path);
                return;
            }

            String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, (String) null);
            String[] invalidCacheEntries = properties.get(InvalidateCacheSupport.PROPERTIES_INVALID_CACHE_ENTRIES, String[].class);
            String type = properties.get(InvalidateCacheSupport.PROPERTIES_TYPE, String.class);

            GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);
            String dataString = formatList(invalidCacheEntries, ", ", "\"%s\"");

            String[] invalidateDispatcherPagePaths = new String[0];
            String[] correspondingPaths = new String[0];

            if (InvalidateCacheSupport.TYPE_SKU.equals(type)) {
                invalidateDispatcherPagePaths = getCorrespondingProductsPageBasedOnSku(session, storePath, invalidCacheEntries);
                String query = generateSkuQuery(dataString);
                Map<String, Object> data = getGraphqlResponseData(client, query);
                if (data != null && data.get("products") != null) {
                    correspondingPaths = getSkuBasedInvalidPaths(resourceResolver, data, commerceProperties, storePath);
                }
            } else if (InvalidateCacheSupport.TYPE_CATEGORY.equals(type)) {
                invalidateDispatcherPagePaths = getCorrespondingCategoryPageBasedOnUid(session, storePath, invalidCacheEntries);
                String query = generateCategoryQuery(dataString);
                Map<String, Object> data = getGraphqlResponseData(client, query);
                if (data != null && data.get("categoryList") != null) {
                    correspondingPaths = getCategoryBasedInvalidPaths(resourceResolver, data, commerceProperties, storePath);
                }
            }

            String[] allPaths = Stream.concat(Arrays.stream(invalidateDispatcherPagePaths), Arrays.stream(correspondingPaths))
                .toArray(String[]::new);

            for (String dispatcherPath : allPaths) {
                flushCache(dispatcherPath);
            }
        } catch (LoginException e) {
            LOGGER.error("Error getting service user: {}", e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] getSkuBasedInvalidPaths(ResourceResolver resourceResolver, Map<String, Object> data,
        ComponentsConfiguration commerceProperties,
        String storePath) throws RepositoryException {
        Page page = getPage(resourceResolver, storePath);

        Set<String> uniquePagePaths = new HashSet<>();
        String categoryPath = getCorrespondingPageProperties(resourceResolver, storePath, "cq:cifCategoryPage");
        String categoryPageUrlFormat = getPageFormatUrl(commerceProperties, PROPERTY_CATEGORY_PAGE_URL_FORMAT);

        categoryPageUrlFormat = truncateUrlFormat(categoryPageUrlFormat);

        List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map<String, Object>) data.get("products")).get("items");
        Map<String, String> params = new HashMap<>();

        for (Map<String, Object> item : items) {
            params.put("url_path", (String) item.get("url_path"));
            params.put("url_key", (String) item.get("url_key"));
            params.put("sku", (String) item.get("sku"));

            String productUrlPath = urlProvider.toProductUrl(null, page, params);

            String categoryUrlPath = categoryPageUrlFormat.replace("{{page}}", categoryPath);

            List<Map<String, String>> urlRewrites = (List<Map<String, String>>) item.get("url_rewrites");
            if (urlRewrites != null) {
                for (Map<String, String> urlRewrite : urlRewrites) {
                    String productUrl = urlRewrite.get("url").replace(".html", "");
                    String[] parts = productUrl.split("/");
                    if (parts.length > 1) {
                        String categoryUrl = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                        uniquePagePaths.add(categoryUrlPath.replace("{{url_path}}", categoryUrl));
                    }
                    uniquePagePaths.add(productUrlPath.replace("{{url_path}}", productUrl));
                }
            }
        }
        return uniquePagePaths.toArray(new String[0]);
    }

    private static String truncateUrlFormat(String urlFormat) {
        if (urlFormat != null && urlFormat.endsWith("/")) {
            return urlFormat.substring(0, urlFormat.length() - 1);
        }
        return urlFormat;
    }

    private static String[] getCategoryBasedInvalidPaths(ResourceResolver resourceResolver, Map<String, Object> data,
        ComponentsConfiguration commerceProperties,
        String storePath) throws RepositoryException {
        Set<String> uniquePagePaths = new HashSet<>();
        String productPath = getCorrespondingPageProperties(resourceResolver, storePath, "cq:cifProductPage");
        String productPageUrlFormat = getPageFormatUrl(commerceProperties, PROPERTY_PRODUCT_PAGE_URL_FORMAT);
        String categoryPath = getCorrespondingPageProperties(resourceResolver, storePath, "cq:cifCategoryPage");
        String categoryPageUrlFormat = getPageFormatUrl(commerceProperties, PROPERTY_CATEGORY_PAGE_URL_FORMAT);

        productPageUrlFormat = truncateUrlFormat(productPageUrlFormat);
        categoryPageUrlFormat = truncateUrlFormat(categoryPageUrlFormat);

        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("categoryList");
        for (Map<String, Object> item : items) {
            String productUrlPath = productPageUrlFormat.replace("{{page}}", productPath)
                .replace("{{url_key}}", (String) item.get("url_key"))
                .replace("{{url_path}}", (String) item.get("url_path"));
            String categoryUrlPath = categoryPageUrlFormat.replace("{{page}}", categoryPath)
                .replace("{{url_key}}", (String) item.get("url_key"))
                .replace("{{url_path}}", (String) item.get("url_path"));
            uniquePagePaths.add(productUrlPath);
            uniquePagePaths.add(categoryUrlPath);
        }
        return uniquePagePaths.toArray(new String[0]);
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
        return null;
    }

    private static boolean isValid(ValueMap valueMap, ResourceResolver resourceResolver, ComponentsConfiguration commerceProperties,
        String storePath) {
        Map<String, Map<String, Object>> jsonData = createJsonData(resourceResolver, commerceProperties, storePath);
        for (Map.Entry<String, Map<String, Object>> entry : jsonData.entrySet()) {
            Map<String, Object> properties = entry.getValue();
            String key = entry.getKey();
            boolean isFunction = (boolean) properties.get("isFunction");
            if (isFunction) {
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

                    if (result == null) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            } else {
                boolean isFlag = true;
                Class<?> clazz = (Class<?>) properties.get("class");
                Object value = getPropertiesValue(valueMap, key, clazz);
                if (value instanceof String) {
                    isFlag = !((String) value).isEmpty();
                } else if (value instanceof Object[]) {
                    isFlag = !(((Object[]) value).length == 0);
                }
                if (!isFlag) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<String, Map<String, Object>> createJsonData(ResourceResolver resourceResolver,
        ComponentsConfiguration commerceProperties,
        String actualStorePath) {
        Map<String, Map<String, Object>> jsonData = new HashMap<>();

        jsonData.put(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, createProperty(false, String.class));
        jsonData.put(InvalidateCacheSupport.PROPERTIES_INVALID_CACHE_ENTRIES, createProperty(false, String[].class));
        jsonData.put(InvalidateCacheSupport.PROPERTIES_STORE_PATH, createProperty(false, String.class));
        jsonData.put(InvalidateCacheSupport.PROPERTIES_TYPE, createProperty(false, String.class));
        jsonData.put("categoryPath", createFunctionProperty("getCorrespondingPageProperties", new Class<?>[] { ResourceResolver.class,
            String.class, String.class },
            new Object[] { resourceResolver, actualStorePath, "cq:cifCategoryPage" }));
        jsonData.put("productPath", createFunctionProperty("getCorrespondingPageProperties", new Class<?>[] { ResourceResolver.class,
            String.class, String.class },
            new Object[] { resourceResolver, actualStorePath, "cq:cifProductPage" }));
        jsonData.put(PROPERTY_PRODUCT_PAGE_URL_FORMAT, createFunctionProperty("getPageFormatUrl", new Class<?>[] {
            ComponentsConfiguration.class,
            String.class },
            new Object[] { commerceProperties, PROPERTY_PRODUCT_PAGE_URL_FORMAT }));
        jsonData.put(PROPERTY_CATEGORY_PAGE_URL_FORMAT, createFunctionProperty("getPageFormatUrl", new Class<?>[] {
            ComponentsConfiguration.class,
            String.class },
            new Object[] { commerceProperties, PROPERTY_CATEGORY_PAGE_URL_FORMAT }));

        return jsonData;
    }

    private static Map<String, Object> createProperty(boolean isFunction, Class<?> clazz) {
        Map<String, Object> property = new HashMap<>();
        property.put("isFunction", isFunction);
        property.put("class", clazz);
        return property;
    }

    private static Map<String, Object> createFunctionProperty(String method, Class<?>[] parameterTypes, Object[] args) {
        Map<String, Object> property = new HashMap<>();
        property.put("isFunction", true);
        property.put("method", method);
        property.put("parameterTypes", parameterTypes);
        property.put("args", args);
        return property;
    }

    private static Object getPropertiesValue(ValueMap properties, String key, Object clazz) {
        return properties.get(key, clazz);
    }

    private static <T> T getPropertiesValue(ValueMap properties, String key, Class<T> clazz) {
        return properties.get(key, clazz);
    }

    private static Page getPage(ResourceResolver resourceResolver, String storePath) throws RepositoryException {
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

    private static String generateSkuQuery(String skuString) {

        return "{\n" +
            "  products(\n" +
            "    filter: { sku: { in: [" + skuString + "] } }\n" +
            "    pageSize: 100\n" +
            "    currentPage: 1\n" +
            "  ) {\n" +
            "    items {\n" +
            "      sku\n" +
            "      url_key\n" +
            "      url_path\n" +
            "      url_rewrites {\n" +
            "        url\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    }

    private static String generateCategoryQuery(String uidString) {

        return "{\n" +
            "  categoryList(\n" +
            "    filters: { category_uid: { in: [" + uidString + "] } }\n" +
            "  ) {\n" +
            "    url_path\n" +
            "    url_key\n" +
            "  }\n" +
            "}";
    }

    private static String formatList(String[] invalidCacheEntries, String delimiter, String pattern) {
        return Arrays.stream(invalidCacheEntries)
            .map(item -> String.format(pattern, item))
            .collect(Collectors.joining(delimiter));
    }

    private static String getSkuBasedSql2Query(String storePath, String skuList) {
        return "SELECT content.[jcr:path] " +
            "FROM [cq:Page] AS page " +
            "INNER JOIN [nt:unstructured] AS content ON ISDESCENDANTNODE(content, page) " +
            "WHERE ISDESCENDANTNODE(page,'" + storePath + "' ) " +
            "AND (" +
            "(content.[product] in (" + skuList + ") AND content.[productType] in ('combinedSku')) " +
            "OR (content.[selection] in (" + skuList + ") AND content.[selectionType] in ('combinedSku', 'sku'))" +
            ")";
    }

    private static String getCategoryBasedSql2Query(Session session, String storePath, String categoryList) throws RepositoryException {
        return "SELECT content.[jcr:path] " +
            "FROM [cq:Page] AS page " +
            "INNER JOIN [nt:unstructured] AS content ON ISDESCENDANTNODE(content, page) " +
            "WHERE ISDESCENDANTNODE(page,'" + storePath + "' ) " +
            "AND (" +
            "(content.[categoryId] in (" + categoryList + ") AND content.[categoryIdType] in ('uid')) " +
            "OR (content.[category] in (" + categoryList + ") AND content.[categoryType] in ('uid'))" +
            ")";
    }

    private String[] getCorrespondingProductsPageBasedOnSku(Session session, String storePath, String[] invalidCacheEntries)
        throws Exception {
        String skuList = formatList(invalidCacheEntries, ", ", "'%s'");
        String sql2Query = getSkuBasedSql2Query(storePath, skuList);
        return getCorrespondingPageBasedOnQuery(session, sql2Query);
    }

    private String[] getCorrespondingCategoryPageBasedOnUid(Session session, String storePath, String[] invalidCacheEntries)
        throws Exception {
        String categoryList = formatList(invalidCacheEntries, ", ", "'%s'");
        String sql2Query = getCategoryBasedSql2Query(session, storePath, categoryList);
        return getCorrespondingPageBasedOnQuery(session, sql2Query);
    }

    private String[] getCorrespondingPageBasedOnQuery(Session session, String queryString)
        throws Exception {
        Set<String> uniquePagePaths = new HashSet<>();
        Query query = session.getWorkspace().getQueryManager().createQuery(queryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        RowIterator rows = result.getRows();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            String fullPath = row.getPath("content");
            String pagePath = extractPagePath(fullPath) + ".html";
            uniquePagePaths.add(pagePath);
        }
        return uniquePagePaths.toArray(new String[0]);
    }

    private String extractPagePath(String fullPath) {
        int jcrContentIndex = fullPath.indexOf("/jcr:content");
        return jcrContentIndex != -1 ? fullPath.substring(0, jcrContentIndex) : fullPath;
    }

    private static String getPageFormatUrl(ComponentsConfiguration commerceProperties, String propertyName) {
        String url = commerceProperties.get(propertyName, (String) null);
        // Find the last occurrence of ".html" to include the entire relevant path
        int htmlIndex = url.lastIndexOf(".html");
        if (htmlIndex != -1) {
            // Include everything up to the last ".html"
            url = url.substring(0, htmlIndex + 5);
        }
        return url;
    }

    private static void flushCache(String handle) {
        try {
            String server = "localhost:80";
            String uri = "/dispatcher/invalidate.cache";

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod("http://" + server + uri);
            post.setRequestHeader("CQ-Action", "Delete");
            post.setRequestHeader("CQ-Handle", handle);
            post.setRequestHeader("CQ-Action-Scope", "ResourceOnly");

            client.executeMethod(post);
            System.out.println("Response: " + post.getResponseBodyAsString());
            post.releaseConnection();
            // log the results
            LOGGER.info("result: {}", post.getResponseBodyAsString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOGGER.error("Flushcache servlet exception: {}", e.getMessage());
        }
    }
}
