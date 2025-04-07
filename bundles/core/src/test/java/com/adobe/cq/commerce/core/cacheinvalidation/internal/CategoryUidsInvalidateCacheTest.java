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

import java.lang.reflect.Method;
import java.util.*;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CategoryUidsInvalidateCacheTest {

    private CategoryUidsInvalidateCache categoryUidsInvalidateCache;

    @Mock
    private UrlProviderImpl urlProvider;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private CacheInvalidationContext mockContext;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Session session;

    @Mock
    private QueryManager queryManager;

    @Mock
    private Query query;

    @Mock
    private QueryResult queryResult;

    @Mock
    private NodeIterator nodeIterator;

    @Mock
    private Node node;

    @Mock
    private Page page;

    @Mock
    private MagentoGraphqlClient graphqlClient;

    @Mock
    private GraphqlResponse<com.adobe.cq.commerce.magento.graphql.Query, Error> graphqlResponse;

    @Mock
    private RowIterator rowIterator;

    @Mock
    private Row row;

    @Mock
    private javax.jcr.Workspace workspace;

    private static final String TEST_STORE_PATH = "/content/venia/us/en";
    private static final String TEST_CATEGORY_UID = "test-category-uid";
    private static final String TEST_CATEGORY_PATH = "/content/venia/us/en/categories/test-category";

    @Before
    public void setup() throws RepositoryException, InvalidQueryException {
        categoryUidsInvalidateCache = new CategoryUidsInvalidateCache();
        // Use reflection to set private fields
        try {
            java.lang.reflect.Field urlProviderField = CategoryUidsInvalidateCache.class.getDeclaredField("urlProvider");
            urlProviderField.setAccessible(true);
            urlProviderField.set(categoryUidsInvalidateCache, urlProvider);

            java.lang.reflect.Field invalidateCacheSupportField = CategoryUidsInvalidateCache.class.getDeclaredField(
                "invalidateCacheSupport");
            invalidateCacheSupportField.setAccessible(true);
            invalidateCacheSupportField.set(categoryUidsInvalidateCache, invalidateCacheSupport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private fields", e);
        }

        when(mockContext.getGraphqlClient()).thenReturn(graphqlClient);
        when(mockContext.getResourceResolver()).thenReturn(resourceResolver);
        when(mockContext.getStorePath()).thenReturn(TEST_STORE_PATH);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getNodes()).thenReturn(nodeIterator);
        when(nodeIterator.hasNext()).thenReturn(true, false); // Return true once, then false
        when(nodeIterator.nextNode()).thenReturn(node);
        when(node.getPath()).thenReturn(TEST_CATEGORY_PATH);
        when(resourceResolver.getResource(TEST_CATEGORY_PATH)).thenReturn(null); // Simulate resource not found
        when(graphqlClient.execute(anyString())).thenReturn(graphqlResponse);
        when(graphqlResponse.getData()).thenReturn(mock(com.adobe.cq.commerce.magento.graphql.Query.class));
    }

    @Test
    public void testGetPatterns() {
        String[] invalidationParameters = { "uid1", "uid2" };
        List<String> patterns = categoryUidsInvalidateCache.getPatterns(invalidationParameters);
        assertEquals(1, patterns.size());
        assertEquals("\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"(uid1|uid2)", patterns.get(0));
    }

    @Test
    public void testGetPathsToInvalidateWithEmptyCategoryUids() {
        when(mockContext.getInvalidationParameters()).thenReturn(Collections.emptyList());

        List<String> paths = categoryUidsInvalidateCache.getPathsToInvalidate(mockContext);

        assertNotNull(paths);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithNullInvalidationParameters() {
        when(mockContext.getInvalidationParameters()).thenReturn(null);

        List<String> paths = categoryUidsInvalidateCache.getPathsToInvalidate(mockContext);

        assertNotNull(paths);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithGraphQLError() throws Exception {
        when(mockContext.getInvalidationParameters()).thenReturn(Collections.singletonList(TEST_CATEGORY_UID));
        when(mockContext.getGraphqlClient()).thenReturn(graphqlClient);
        when(graphqlClient.execute(anyString())).thenThrow(new RuntimeException("GraphQL Error"));

        List<String> paths = categoryUidsInvalidateCache.getPathsToInvalidate(mockContext);

        assertNotNull(paths);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithJCRQueryError() throws Exception {
        when(mockContext.getInvalidationParameters()).thenReturn(Collections.singletonList(TEST_CATEGORY_UID));
        when(queryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenThrow(new RepositoryException("JCR Query Error"));

        List<String> paths = categoryUidsInvalidateCache.getPathsToInvalidate(mockContext);

        assertNotNull(paths);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithEmptyGraphQLResponse() throws Exception {
        when(mockContext.getInvalidationParameters()).thenReturn(Collections.singletonList(TEST_CATEGORY_UID));
        when(mockContext.getGraphqlClient()).thenReturn(graphqlClient);
        when(graphqlClient.execute(anyString())).thenReturn(graphqlResponse);
        when(graphqlResponse.getData()).thenReturn(mock(com.adobe.cq.commerce.magento.graphql.Query.class));

        List<String> paths = categoryUidsInvalidateCache.getPathsToInvalidate(mockContext);

        assertNotNull(paths);
        assertEquals(0, paths.size());
    }

    private void invokePrivateMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    @Test
    public void testAddJcrPaths() throws Exception {
        Set<String> allPaths = new HashSet<>();
        String[] categoryUids = { TEST_CATEGORY_UID };

        invokePrivateMethod(categoryUidsInvalidateCache, "addJcrPaths", new Class<?>[] { CacheInvalidationContext.class,
            String[].class, Set.class },
            mockContext, categoryUids, allPaths);
    }

    @Test
    public void testAddGraphqlPaths() throws Exception {
        Set<String> allPaths = new HashSet<>();
        Map<String, Object> category = new HashMap<>();
        category.put("uid", TEST_CATEGORY_UID);
        category.put("urlKey", "category-url-key");
        category.put("urlPath", "category-url-path");
        List<Map<String, Object>> categories = Collections.singletonList(category);

        when(mockContext.getPage()).thenReturn(page);
        when(urlProvider.toCategoryUrl(any(), eq(page), anyString())).thenReturn(TEST_CATEGORY_PATH);

        invokePrivateMethod(categoryUidsInvalidateCache, "addGraphqlPaths", new Class<?>[] { CacheInvalidationContext.class,
            List.class, Set.class },
            mockContext, categories, allPaths);
    }

    @Test
    public void testFetchCategories() throws Exception {
        String[] categoryUids = { TEST_CATEGORY_UID };
        com.adobe.cq.commerce.magento.graphql.Query queryData = mock(com.adobe.cq.commerce.magento.graphql.Query.class);
        CategoryTree categoryTree = new CategoryTree();
        categoryTree.setUid(new com.shopify.graphql.support.ID(TEST_CATEGORY_UID));
        categoryTree.setUrlKey("category-url-key");
        categoryTree.setUrlPath("category-url-path");
        when(queryData.getCategoryList()).thenReturn(Collections.singletonList(categoryTree));
        when(graphqlClient.execute(anyString())).thenReturn(graphqlResponse);
        when(graphqlResponse.getData()).thenReturn(queryData);

        Method method = CategoryUidsInvalidateCache.class.getDeclaredMethod("fetchCategories", CacheInvalidationContext.class,
            String[].class);
        method.setAccessible(true);
        List<Map<String, Object>> categories = (List<Map<String, Object>>) method.invoke(categoryUidsInvalidateCache, mockContext,
            categoryUids);

        assertNotNull(categories);
        assertEquals(1, categories.size());
    }

}
