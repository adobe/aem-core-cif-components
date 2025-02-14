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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class InvalidateDispatcherCacheBaseTest {

    @Mock
    private UrlProviderImpl urlProvider;

    @Mock
    private Page page;

    @InjectMocks
    private InvalidateDispatcherCacheBase invalidateDispatcherCacheBase;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddProductPaths() {
        Set<String> uniquePagePaths = new HashSet<>();
        Map<String, Object> item = new HashMap<>();
        item.put("sku", "test-sku");
        item.put("url_key", "test-url-key");
        item.put("url_rewrites", Arrays.asList(Collections.singletonMap("url", "test-url")));

        when(urlProvider.toProductUrl(any(), eq(page), any(ProductUrlFormat.Params.class))).thenReturn("test-url-path");

        invalidateDispatcherCacheBase.addProductPaths(page, urlProvider, uniquePagePaths, item);

        assertEquals(1, uniquePagePaths.size());
        assertEquals("test-url-path", uniquePagePaths.iterator().next());
    }

    @Test
    public void testAddCategoryPaths() {
        Set<String> uniquePagePaths = new HashSet<>();
        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("uid", "test-uid");
        category.put("url_key", "test-url-key");
        category.put("url_path", "test-url-path");
        categories.add(category);

        when(urlProvider.toCategoryUrl(any(), eq(page), any(CategoryUrlFormat.Params.class))).thenReturn("test-category-url-path");

        invalidateDispatcherCacheBase.addCategoryPaths(page, urlProvider, uniquePagePaths, categories);

        assertEquals(1, uniquePagePaths.size());
        assertEquals("test-category-url-path", uniquePagePaths.iterator().next());
    }

    @Test
    public void testProcessItems() {
        Set<String> uniquePagePaths = new HashSet<>();
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("sku", "test-sku");
        item.put("url_key", "test-url-key");
        item.put("url_path", "test-url-path");
        items.add(item);

        when(urlProvider.toProductUrl(any(), eq(page), any(ProductUrlFormat.Params.class))).thenReturn("test-url-path");

        invalidateDispatcherCacheBase.processItems(page, urlProvider, uniquePagePaths, items);

        assertEquals(1, uniquePagePaths.size());
        assertEquals("test-url-path", uniquePagePaths.iterator().next());
    }

    @Test
    public void testRemoveUpToDelimiter() {
        String input = "test-url-path/product-page.html";
        String delimiter = "/";
        String expected = "test-url-path";
        String actual = invalidateDispatcherCacheBase.removeUpToDelimiter(input, delimiter, true);
        assertEquals(expected, actual);
    }
}
