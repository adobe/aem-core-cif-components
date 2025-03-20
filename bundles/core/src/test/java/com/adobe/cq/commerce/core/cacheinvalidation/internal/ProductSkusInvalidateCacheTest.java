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
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.day.cq.wcm.api.Page;
import com.shopify.graphql.support.ID;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProductSkusInvalidateCacheTest {

    @Rule
    public final AemContext context = new AemContext();

    @Mock
    private UrlProviderImpl urlProvider;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private DispatcherCacheInvalidationContext mockContext;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Session session;

    @Mock
    private Workspace workspace;

    @Mock
    private QueryManager queryManager;

    @Mock
    private javax.jcr.query.Query query;

    @Mock
    private QueryResult queryResult;

    @Mock
    private RowIterator rowIterator;

    @Mock
    private Row row;

    @Mock
    private MagentoGraphqlClient graphqlClient;

    @Mock
    private GraphqlResponse<Map<String, Object>, Map<String, Object>> graphqlResponse;

    @Mock
    private Page page;

    @Mock
    private NodeIterator nodeIterator;

    @Mock
    private Node node;

    private ProductSkusInvalidateCache productSkusInvalidateCache;

    private static final String TEST_STORE_PATH = "/content/venia/us/en";
    private static final List<String> TEST_SKUS = Arrays.asList("sku1", "sku2", "sku3");
    private static final String TEST_PRODUCT_PATH = "/content/store/product-page";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        productSkusInvalidateCache = new ProductSkusInvalidateCache();

        // Set up dependencies
        context.registerService(UrlProviderImpl.class, urlProvider);
        context.registerService(InvalidateCacheSupport.class, invalidateCacheSupport);
        context.registerInjectActivateService(productSkusInvalidateCache);

        // Set up context mocks
        when(mockContext.getResourceResolver()).thenReturn(resourceResolver);
        when(mockContext.getStorePath()).thenReturn(TEST_STORE_PATH);
        when(mockContext.getGraphqlClient()).thenReturn(graphqlClient);
        when(mockContext.getPage()).thenReturn(page);
        when(mockContext.getAttributeData()).thenReturn(TEST_SKUS);

        // Set up JCR query mocks
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(javax.jcr.query.Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
    }

    @Test
    public void testGetPattern() {
        assertEquals("\"sku\":\\s*\"", productSkusInvalidateCache.getPattern());
    }

    @Test
    public void testGetPathsToInvalidateWithNullSkus() {
        when(mockContext.getAttributeData()).thenReturn(null);
        List<String> paths = productSkusInvalidateCache.getPathsToInvalidate(mockContext);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithEmptySkus() {
        when(mockContext.getAttributeData()).thenReturn(Collections.emptyList());
        List<String> paths = productSkusInvalidateCache.getPathsToInvalidate(mockContext);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithGraphQLError() throws Exception {
        when(graphqlClient.execute(anyString())).thenThrow(new RuntimeException("GraphQL Error"));

        List<String> paths = productSkusInvalidateCache.getPathsToInvalidate(mockContext);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithJCRQueryError() throws Exception {
        when(queryManager.createQuery(anyString(), eq(javax.jcr.query.Query.JCR_SQL2)))
            .thenThrow(new RuntimeException("JCR Query Error"));

        List<String> paths = productSkusInvalidateCache.getPathsToInvalidate(mockContext);
        assertEquals(0, paths.size());
    }

    @Test
    public void testGetPathsToInvalidateWithNullGraphQLClient() {
        when(mockContext.getGraphqlClient()).thenReturn(null);
        List<String> paths = productSkusInvalidateCache.getPathsToInvalidate(mockContext);
        assertEquals(0, paths.size());
    }

    @Test(expected = NullPointerException.class)
    public void testGetPathsToInvalidateWithNullContext() {
        productSkusInvalidateCache.getPathsToInvalidate(null);
    }

    private void invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = ProductSkusInvalidateCache.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(productSkusInvalidateCache, args);
    }

    @Test
    public void testAddJcrPath() throws Exception {
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath()).thenReturn(TEST_PRODUCT_PATH);

        Set<String> allPaths = new HashSet<>();
        invokePrivateMethod("addJcrPaths", new Class<?>[] { DispatcherCacheInvalidationContext.class, String[].class, Set.class },
            mockContext, TEST_SKUS.toArray(new String[0]), allPaths);
    }

    @Test
    public void testAddGraphqlPath() throws Exception {
        Map<String, Object> product = new HashMap<>();
        product.put("sku", "sku1");
        product.put("urlKey", "product-url-key");
        product.put("urlRewrites", Collections.emptyList());
        product.put("categories", Collections.emptyList());

        List<Map<String, Object>> products = Collections.singletonList(product);
        Set<String> allPaths = new HashSet<>();
        invokePrivateMethod("addGraphqlPaths", new Class<?>[] { DispatcherCacheInvalidationContext.class, List.class, Set.class },
            mockContext, products, allPaths);
    }

    @Test
    public void testGetCorrespondingPagePas() throws Exception {
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getQueryManager()).thenReturn(queryManager);
        when(queryManager.createQuery(anyString(), eq(javax.jcr.query.Query.JCR_SQL2))).thenReturn(query);
        when(query.execute()).thenReturn(queryResult);
        when(queryResult.getRows()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false);
        when(rowIterator.nextRow()).thenReturn(row);
        when(row.getPath()).thenReturn(TEST_PRODUCT_PATH);

        invokePrivateMethod("getCorrespondingPagePaths", new Class<?>[] { Session.class, String.class, String.class },
            session, TEST_STORE_PATH, "'sku1','sku2','sku3'");
    }

    @Test
    public void testAddGraphqlPathsWithCategories() throws Exception {
        CategoryTree category = mock(CategoryTree.class);
        ID categoryUid = new ID("category-uid");
        when(category.getUid()).thenReturn(categoryUid);
        when(category.getName()).thenReturn("category-name");
        when(category.getUrlPath()).thenReturn("category-url-path");

        List<CategoryTree> categories = Collections.singletonList(category);
        Map<String, Object> product = new HashMap<>();
        product.put("sku", "sku1");
        product.put("urlKey", "product-url-key");
        product.put("urlRewrites", Collections.emptyList());
        product.put("categories", categories);

        List<Map<String, Object>> products = Collections.singletonList(product);
        Set<String> allPaths = new HashSet<>();
        invokePrivateMethod("addGraphqlPaths", new Class<?>[] { DispatcherCacheInvalidationContext.class, List.class, Set.class },
            mockContext, products, allPaths);
    }
}
