/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.day.cq.wcm.api.Page;
import com.google.common.cache.Cache;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CategoryProviderTest {
    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    private CategoryProvider categoryProvider = null;
    private Page page = null;
    private Resource pageContent;

    @Before
    public void setUp() {
        page = spy(context.currentPage("/content/pageA"));
        pageContent = spy(page.getContentResource());
        when(page.getContentResource()).thenReturn(pageContent);
        categoryProvider = new CategoryProvider();
        categoryProvider.activate(new CategoryCacheConfig() {
            @Override
            public boolean enabled() {
                return DEFAULT_ENABLED;
            }

            @Override
            public int maxSize() {
                return DEFAULT_MAX_SIZE;
            }

            @Override
            public int expirationMinutes() {
                return DEFAULT_EXPIRATION_MINUTES;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CategoryCacheConfig.class;
            }
        });
    }

    @After
    public void tearDown() {
        categoryProvider.deactivate();
    }

    @Test
    public void testMissingMagentoGraphqlClient() throws IOException {
        Assert.assertTrue(categoryProvider.getChildCategories(10, 2, null, page).isEmpty());
    }

    @Test
    public void testNoCategoryOrNoChildren() throws IOException {
        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-none.json");
        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        List<CategoryTree> categories = categoryProvider.getChildCategories(10, 2, null, page);
        Assert.assertEquals(0, categories.size());

        graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-empty.json");
        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        categories = categoryProvider.getChildCategories(8, 64, null, page);
        Assert.assertEquals(0, categories.size());
    }

    @Test
    public void testGetChildCategories() throws IOException {
        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-result.json");
        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        // Test null categoryId
        Assert.assertTrue(categoryProvider.getChildCategories(null, 5, null, page).isEmpty());

        // Test category children found
        List<CategoryTree> categories = categoryProvider.getChildCategories(10, 2, null, page);
        Assert.assertEquals(6, categories.size());
    }

    @Test
    public void testCategoryCaching() throws IOException {
        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-result.json");
        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Cache cache = (Cache) Whitebox.getInternalState(categoryProvider, "cache");

        Assert.assertNotNull(cache);

        // use these theoretical values for categoryId and depth for better coverage

        List<CategoryTree> categories = categoryProvider.getChildCategories(10, 2, null, page);
        Assert.assertEquals(6, categories.size());
        Assert.assertEquals(1, cache.size());

        graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-result.json");
        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        categories = categoryProvider.getChildCategories(8, 64, null, page);
        Assert.assertEquals(6, categories.size());
        Assert.assertEquals(2, cache.size());

        graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-empty.json");
        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        categories = categoryProvider.getChildCategories(9, 33, null, page);
        Assert.assertTrue(categories.isEmpty());
        Assert.assertEquals(3, cache.size());

        // check null arguments
        categories = categoryProvider.getChildCategories(null, null, null, page);
        Assert.assertTrue(categories.isEmpty());
        Assert.assertEquals(3, cache.size());
        categories = categoryProvider.getChildCategories(null, 1, null, page);
        Assert.assertTrue(categories.isEmpty());
        Assert.assertEquals(3, cache.size());
        categories = categoryProvider.getChildCategories(1, null, null, page);
        Assert.assertTrue(categories.isEmpty());
        Assert.assertEquals(3, cache.size());

        // check cached value
        categories = categoryProvider.getChildCategories(10, 2, null, page);
        Assert.assertEquals(6, categories.size());
        Assert.assertEquals(3, cache.size());
    }

    @Test
    public void testDefineCategoriesQuery() {
        final String coreString = extractQueryString(-1);
        Assert.assertNotNull(coreString);
        Assert.assertTrue(coreString.trim().length() > 0);

        // depth 0
        Assert.assertEquals(coreString, extractQueryString(0));
        checkQueryString(coreString, 0);

        // depth 1
        checkQueryString(coreString, 1);

        // depth 2
        checkQueryString(coreString, 2);

        // depth max structure depth
        checkQueryString(coreString, NavigationImpl.MAX_STRUCTURE_DEPTH);
    }

    private void checkQueryString(String coreString, int depth) {
        String queryString = extractQueryString(depth);
        Assert.assertEquals(depth + 1, StringUtils.countMatches(queryString, coreString));
        Assert.assertTrue(queryString.endsWith(StringUtils.repeat('}', depth)));
    }

    private String extractQueryString(int depth) {
        int categoryId = 0;
        QueryQuery.CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);
        QueryQuery topQuery = Operations.query(query -> query.category(searchArgs, CategoryProvider.defineCategoriesQuery(depth)));
        String queryString = topQuery.toString();

        // Trim "{category(id:0){" from the beginning and "}}" from the end of the string.
        queryString = queryString.substring(16, queryString.length() - 2);
        return queryString;
    }
}
