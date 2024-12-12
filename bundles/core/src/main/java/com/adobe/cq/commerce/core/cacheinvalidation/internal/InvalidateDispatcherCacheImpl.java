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
import javax.jcr.query.*;
import javax.jcr.query.Query;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.reflect.TypeToken;

@Component(service = InvalidateDispatcherCacheImpl.class, immediate = true)
public class InvalidateDispatcherCacheImpl {

    @Reference
    private UrlProviderImpl urlProvider;

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateDispatcherCacheImpl.class);

    public void invalidateCache(String path) {
        // To Do: Change this to for non-author run modes
        if (!slingSettingsService.getRunModes().contains("author")) {
            LOGGER.error("Operation is only supported for author");
            return;
        }
        try (ResourceResolver resourceResolver = invalidateCacheSupport.getServiceUserResourceResolver()) {
            Resource resource = invalidateCacheSupport.getResource(resourceResolver, path);
            if (resource == null)
                return;

            Session session = getSession(resourceResolver);
            if (session == null)
                return;

            ValueMap properties = resource.getValueMap();
            String storePath = properties.get(InvalidateCacheSupport.PROPERTIES_STORE_PATH, String.class);
            ComponentsConfiguration commerceProperties = getCommerceProperties(resourceResolver, storePath);
            if (!isValid(properties, resourceResolver, commerceProperties, storePath))
                return;

            String graphqlClientId = commerceProperties.get(InvalidateCacheSupport.PROPERTIES_GRAPHQL_CLIENT_ID, (String) null);
            Map<String, String[]> dynamicProperties = new HashMap<>();
            dynamicProperties.put(InvalidateCacheSupport.PROPERTIES_PRODUCT_SKUS, properties.get(
                InvalidateCacheSupport.PROPERTIES_PRODUCT_SKUS, String[].class));
            dynamicProperties.put(InvalidateCacheSupport.PROPERTIES_CATEGORY_UIDS, properties.get(
                InvalidateCacheSupport.PROPERTIES_CATEGORY_UIDS, String[].class));

            GraphqlClient client = invalidateCacheSupport.getClient(graphqlClientId);

            String[] allPaths = getAllInvalidPaths(session, resourceResolver, client, commerceProperties, storePath, dynamicProperties);

            Arrays.stream(allPaths).forEach(this::flushCache);
        } catch (Exception e) {
            LOGGER.error("Error invalidating cache: {}", e.getMessage(), e);
        }
    }

    private Session getSession(ResourceResolver resourceResolver) {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            LOGGER.error("Session not found for resource resolver");
        }
        return session;
    }

    private ComponentsConfiguration getCommerceProperties(ResourceResolver resourceResolver, String storePath) {
        return invalidateCacheSupport.getCommerceProperties(resourceResolver, storePath);
    }

    private String[] getAllInvalidPaths(Session session, ResourceResolver resourceResolver, GraphqlClient client,
        ComponentsConfiguration commerceProperties,
        String storePath, Map<String, String[]> dynamicProperties) throws Exception {
        String[] invalidateDispatcherPagePaths = new String[0];
        String[] correspondingPaths = new String[0];

        for (Map.Entry<String, String[]> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            if (values != null && values.length > 0) {
                String[] paths = getCorrespondingPageBasedOnEntries(session, storePath, values, key);
                String query = generateQuery(values, key);
                Map<String, Object> data = getGraphqlResponseData(client, query);
                if (data != null) {
                    String[] invalidPaths = getInvalidPaths(resourceResolver, data, commerceProperties, storePath, key);
                    correspondingPaths = Stream.concat(Arrays.stream(correspondingPaths), Arrays.stream(invalidPaths))
                        .toArray(String[]::new);
                }
                invalidateDispatcherPagePaths = Stream.concat(Arrays.stream(invalidateDispatcherPagePaths), Arrays.stream(paths))
                    .toArray(String[]::new);
            }
        }

        return Stream.concat(Arrays.stream(invalidateDispatcherPagePaths), Arrays.stream(correspondingPaths))
            .toArray(String[]::new);
    }

    private String[] getCorrespondingPageBasedOnEntries(Session session, String storePath, String[] entries, String key) throws Exception {
        String entryList = formatList(entries, ", ", "'%s'");
        if ("productSkus".equals(key)) {
            return getQueryResult(getSkuBasedSql2Query(session, storePath, entryList));
        } else if ("categoryUids".equals(key)) {
            return getQueryResult(getCategoryBasedSql2Query(session, storePath, entryList));
        }
        return new String[0];
    }

    private String generateQuery(String[] entries, String key) {
        if ("productSkus".equals(key)) {
            return generateSkuQuery(entries);
        } else if ("categoryUids".equals(key)) {
            return generateCategoryQuery(entries);
        }
        return "";
    }

    private String[] getInvalidPaths(ResourceResolver resourceResolver, Map<String, Object> data,
        ComponentsConfiguration commerceProperties,
        String storePath, String key) throws RepositoryException {
        if ("productSkus".equals(key)) {
            return getSkuBasedInvalidPaths(resourceResolver, data, commerceProperties, storePath);
        } else if ("categoryUids".equals(key)) {
            return getCategoryBasedInvalidPaths(resourceResolver, data, commerceProperties, storePath);
        }
        return new String[0];
    }

    private String[] getSkuBasedInvalidPaths(ResourceResolver resourceResolver, Map<String, Object> data,
        ComponentsConfiguration commerceProperties, String storePath) throws RepositoryException {
        Page page = getPage(resourceResolver, storePath);
        Set<String> uniquePagePaths = new HashSet<>();

        List<Map<String, Object>> items = (List<Map<String, Object>>) ((Map<String, Object>) data.get("products")).get("items");

        for (Map<String, Object> item : items) {
            addProductPaths(uniquePagePaths, item, page);
            List<Map<String, String>> categories = (List<Map<String, String>>) item.get("categories");
            addCategoryPaths(uniquePagePaths, categories, page);
        }
        return uniquePagePaths.toArray(new String[0]);
    }

    private void addProductPaths(Set<String> uniquePagePaths, Map<String, Object> item, Page page) {
        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();
        productParams.setSku((String) item.get("sku"));
        productParams.setUrlKey((String) item.get("url_key"));
        productParams.setUrlPath((String) item.get("url_path"));

        List<Map<String, String>> urlRewrites = (List<Map<String, String>>) item.get("url_rewrites");
        if (urlRewrites != null) {
            for (Map<String, String> urlRewrite : urlRewrites) {
                productParams.setUrlRewrites(Collections.singletonList(new UrlRewrite().setUrl(urlRewrite.get("url"))));
                String productUrlPath = urlProvider.toProductUrl(null, page, productParams);
                uniquePagePaths.add(productUrlPath);
            }
        }
    }

    private void addCategoryPaths(Set<String> uniquePagePaths, List<Map<String, String>> categories, Page page) {
        CategoryUrlFormat.Params categoryParams = new CategoryUrlFormat.Params();
        if (categories != null) {
            for (Map<String, String> category : categories) {
                categoryParams.setUid(category.get("uid"));
                categoryParams.setUrlKey(category.get("url_key"));
                categoryParams.setUrlPath(category.get("url_path"));
                String categoryUrlPath = urlProvider.toCategoryUrl(null, page, categoryParams);
                uniquePagePaths.add(categoryUrlPath);
            }
        }
    }

    private String[] getCategoryBasedInvalidPaths(ResourceResolver resourceResolver, Map<String, Object> data,
        ComponentsConfiguration commerceProperties,
        String storePath) throws RepositoryException {
        Page page = getPage(resourceResolver, storePath);
        Set<String> uniquePagePaths = new HashSet<>();

        List<Map<String, String>> items = (List<Map<String, String>>) data.get("categoryList");
        addCategoryPaths(uniquePagePaths, items, page);
        ProductUrlFormat.Params productParams = new ProductUrlFormat.Params();

        for (Map<String, String> item : items) {
            // To-Do: For now the below one is an hack to get the product page path,
            // we need to find a better way to get the product page path
            productParams.setUrlKey(item.get("url_key"));
            productParams.setUrlPath(item.get("url_path"));

            // For now, we are not using the below code, but we can use it in future
            // productParams.getCategoryUrlParams().setUid(item.get("uid"));
            // productParams.getCategoryUrlParams().setUrlKey(item.get("url_key"));
            // productParams.getCategoryUrlParams().setUrlPath(item.get("url_path"));

            String productUrlPath = urlProvider.toProductUrl(null, page, productParams);
            uniquePagePaths.add(productUrlPath);
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

    private static String generateSkuQuery(String[] skus) {
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
        FilterEqualTypeInput skuFilter = new FilterEqualTypeInput().setIn(Arrays.asList(skus));
        filter.setSku(skuFilter);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        ProductsQueryDefinition queryArgs = q -> q.items(item -> {
            item.sku()
                .urlKey()
                .urlPath()
                .urlRewrites(uq -> uq.url())
                .categories(c -> c.uid().urlKey().urlPath());
        });
        return Operations.query(query -> query
            .products(searchArgs, queryArgs)).toString();
    }

    private static String generateCategoryQuery(String[] uids) {
        CategoryFilterInput filter = new CategoryFilterInput();
        FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(Arrays.asList(uids));
        filter.setCategoryUid(identifiersFilter);
        QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);

        CategoryTreeQueryDefinition queryArgs = q -> {
            q.uid().name().urlKey().urlPath();
        };

        return Operations.query(query -> query
            .categoryList(searchArgs, queryArgs)).toString();
    }

    private static String formatList(String[] invalidCacheEntries, String delimiter, String pattern) {
        return Arrays.stream(invalidCacheEntries)
            .map(item -> String.format(pattern, item))
            .collect(Collectors.joining(delimiter));
    }

    private static Query getSkuBasedSql2Query(Session session, String storePath, String skuListString) throws Exception {
        QueryManager queryManager = session.getWorkspace().getQueryManager();

        String sql2Query = "SELECT content.[jcr:path] " +
            "FROM [cq:Page] AS page " +
            "INNER JOIN [nt:unstructured] AS content ON ISDESCENDANTNODE(content, page) " +
            "WHERE ISDESCENDANTNODE(page, '" + storePath + "') " +
            "AND ( " +
            "    (content.[product] IN (" + skuListString + ") AND content.[productType] = 'combinedSku') " +
            "    OR (content.[selection] IN (" + skuListString + ") AND content.[selectionType] IN ('combinedSku', 'sku')) " +
            ")";

        return queryManager.createQuery(sql2Query, Query.JCR_SQL2);
    }

    private static Query getCategoryBasedSql2Query(Session session, String storePath, String categoryList) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();

        String sql2Query = "SELECT content.[jcr:path] " +
            "FROM [cq:Page] AS page " +
            "INNER JOIN [nt:unstructured] AS content ON ISDESCENDANTNODE(content, page) " +
            "WHERE ISDESCENDANTNODE(page,'" + storePath + "' ) " +
            "AND (" +
            "(content.[categoryId] in (" + categoryList + ") AND content.[categoryIdType] in ('uid')) " +
            "OR (content.[category] in (" + categoryList + ") AND content.[categoryType] in ('uid'))" +
            ")";
        return queryManager.createQuery(sql2Query, Query.JCR_SQL2);
    }

    private String[] getCorrespondingProductsPageBasedOnSku(Session session, String storePath, String[] invalidCacheEntries)
        throws Exception {
        String skuList = formatList(invalidCacheEntries, ", ", "'%s'");
        return getQueryResult(getSkuBasedSql2Query(session, storePath, skuList));
    }

    private String[] getCorrespondingCategoryPageBasedOnUid(Session session, String storePath, String[] invalidCacheEntries)
        throws Exception {
        String categoryList = formatList(invalidCacheEntries, ", ", "'%s'");
        return getQueryResult(getCategoryBasedSql2Query(session, storePath, categoryList));
    }

    private String[] getQueryResult(Query query)
        throws Exception {
        Set<String> uniquePagePaths = new HashSet<>();
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

    private void flushCache(String handle) {
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
