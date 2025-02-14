/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.*;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProductSkusInvalidateCacheTest {

    @Mock
    private UrlProviderImpl urlProvider;

    @Mock
    private Page page;

    @Mock
    private ResourceResolver resourceResolver;

    @InjectMocks
    private ProductSkusInvalidateCache productSkusInvalidateCache;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPattern() {
        String pattern = productSkusInvalidateCache.getPattern();
        assertEquals("\"sku\":\\s*\"", pattern);
    }

    @Test
    public void testGetQuery() {
        String storePath = "/content/store";
        String dataList = "'sku1', 'sku2'";
        String expectedQuery = "SELECT content.[jcr:path] " +
            "FROM [nt:unstructured] AS content " +
            "WHERE ISDESCENDANTNODE(content, '" + storePath + "') " +
            "AND ( " +
            "    (content.[product] IN (" + dataList + ") AND content.[productType] = 'combinedSku') " +
            "    OR (content.[selection] IN (" + dataList + ") AND content.[selectionType] IN ('combinedSku', 'sku')) " +
            ")";
        String query = productSkusInvalidateCache.getQuery(storePath, dataList);
        assertEquals(expectedQuery, query);
    }

    @Test
    public void testGetGraphqlQuery() {
        String[] data = { "sku1", "sku2" };
        String graphqlQuery = productSkusInvalidateCache.getGraphqlQuery(data);
        assertTrue(graphqlQuery.contains("sku"));
        assertTrue(graphqlQuery.contains("url_key"));
        assertTrue(graphqlQuery.contains("url_rewrites"));
        assertTrue(graphqlQuery.contains("categories"));
    }

    @Test
    public void testGetPathsToInvalidate() {
        Map<String, Object> data = createTestData();
        Set<String> expectedPaths = createExpectedPaths();

        when(urlProvider.toProductUrl(eq(null), eq(page), any(ProductUrlFormat.Params.class)))
            .thenAnswer(invocation -> {
                ProductUrlFormat.Params params = invocation.getArgumentAt(2, ProductUrlFormat.Params.class);
                if ("sku1".equals(params.getSku())) {
                    return "/content/page/path1";
                } else if ("sku2".equals(params.getSku())) {
                    return "/content/page/path2";
                }
                return null;
            });

        String[] paths = productSkusInvalidateCache.getPathsToInvalidate(page, resourceResolver, data, "/content/store");
        assertArrayEquals(expectedPaths.toArray(new String[0]), paths);
    }

    private Map<String, Object> createTestData() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> productsData = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        items.add(createItem("sku1", "/content/page/path1"));
        items.add(createItem("sku2", "/content/page/path2"));

        productsData.put("items", items);
        data.put("products", productsData);
        return data;
    }

    private Map<String, Object> createItem(String sku, String url) {
        Map<String, Object> item = new HashMap<>();
        item.put("sku", sku);
        List<Map<String, Object>> urlRewrites = new ArrayList<>();
        Map<String, Object> urlRewrite = new HashMap<>();
        urlRewrite.put("url", url);
        urlRewrites.add(urlRewrite);
        item.put("url_rewrites", urlRewrites);
        return item;
    }

    private Set<String> createExpectedPaths() {
        Set<String> expectedPaths = new HashSet<>();
        expectedPaths.add("/content/page/path1");
        expectedPaths.add("/content/page/path2");
        return expectedPaths;
    }
}
