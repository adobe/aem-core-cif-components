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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.*;

import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.day.cq.wcm.api.Page;

public class CategoryUidsInvalidateCacheTest {

    @Mock
    private UrlProviderImpl urlProvider;

    @Mock
    private Page page;

    @Mock
    private ResourceResolver resourceResolver;

    @InjectMocks
    private CategoryUidsInvalidateCache categoryUidsInvalidateCache;

    @Mock
    private InvalidateDispatcherCacheBase invalidateDispatcherCacheBase;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPattern() {
        String expectedPattern = "\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
        String actualPattern = categoryUidsInvalidateCache.getPattern();
        assertEquals(expectedPattern, actualPattern);
    }

    @Test
    public void testGetQuery() {
        String storePath = "/content/store";
        String dataList = "'123','456'";
        String expectedQuery = "SELECT content.[jcr:path] " +
                "FROM [nt:unstructured] AS content " +
                "WHERE ISDESCENDANTNODE(content,'" + storePath + "' ) " +
                "AND (" +
                "(content.[categoryId] in (" + dataList + ") AND content.[categoryIdType] in ('uid')) " +
                "OR (content.[category] in (" + dataList + ") AND content.[categoryType] in ('uid'))" +
                ")";
        String actualQuery = categoryUidsInvalidateCache.getQuery(storePath, dataList);
        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void testGetGraphqlQuery() {
        String[] data = {"123", "456"};
        String expectedGraphqlQuery = "{categoryList(filters:{category_uid:{in:[\"123\",\"456\"]}}){uid,name,url_key,url_path}}";
        String actualGraphqlQuery = categoryUidsInvalidateCache.getGraphqlQuery(data);
        assertEquals(expectedGraphqlQuery, actualGraphqlQuery);
    }

    @Test
    public void testGetPathsToInvalidate() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> categoryList = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("uid", "123");
        categoryList.add(category);
        data.put("categoryList", categoryList);

        String storePath = "/content/store";
        String[] expectedPaths = {"/content/store/category/123"};

        when(urlProvider.toCategoryUrl(eq(null), eq(page), any(CategoryUrlFormat.Params.class)))
                .thenReturn("/content/store/category/123");
        doAnswer(invocation -> {
            Set<String> paths = invocation.getArgumentAt(2, Set.class);
            paths.add("/content/store/category/123");
            return null;
        }).when(invalidateDispatcherCacheBase).addCategoryPaths(any(Page.class), any(UrlProviderImpl.class), any(Set.class), anyList());

        String[] actualPaths = categoryUidsInvalidateCache.getPathsToInvalidate(page, resourceResolver, data, storePath);
        assertEquals(Arrays.asList(expectedPaths), Arrays.asList(actualPaths));
    }
}