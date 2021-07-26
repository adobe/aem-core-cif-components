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
package com.adobe.cq.commerce.core.components.internal.models.v1.navigation;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.client.MagentoGraphqlClientImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GraphQLCategoryProviderTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));

    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);
    private static GraphqlClient graphqlClient;

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
                graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-navigation-result.json");
                context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> graphqlClient);
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Test
    public void testMissingMagentoGraphqlClient() throws IOException {
        GraphQLCategoryProvider categoryProvider = new GraphQLCategoryProvider(null);
        Assert.assertTrue(categoryProvider.getChildCategories("Mg==", 10).isEmpty());
    }

    @Test
    public void testCategoryNotOrNoChildren() throws IOException {
        Page page = mock(Page.class);
        Resource pageContent = mock(Resource.class);
        MagentoGraphqlClient graphqlClient = mock(MagentoGraphqlClient.class);
        when(page.getContentResource()).thenReturn(pageContent);
        GraphQLCategoryProvider categoryProvider = new GraphQLCategoryProvider(graphqlClient);

        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);
        Query rootQuery = mock(Query.class);
        List<CategoryTree> list = mock(List.class);
        CategoryTree category = mock(CategoryTree.class);

        // test category not found
        when(graphqlClient.execute(anyString())).thenReturn(response);
        when(response.getData()).thenReturn(rootQuery);
        Assert.assertTrue(categoryProvider.getChildCategories("not-existing", 10).isEmpty());

        // test category found but null
        when(rootQuery.getCategoryList()).thenReturn(list);
        when(rootQuery.getCategoryList().get(0)).thenReturn(null);
        Assert.assertTrue(categoryProvider.getChildCategories("-10", 10).isEmpty());

        // test category children not found
        when(rootQuery.getCategoryList().get(0)).thenReturn(category);
        when(category.getChildren()).thenReturn(null);
        Assert.assertTrue(categoryProvider.getChildCategories("13", 10).isEmpty());
    }

    @Test
    public void testGetChildCategories() throws IOException {
        Page page = spy(context.currentPage("/content/pageA"));
        Resource pageContent = spy(page.getContentResource());
        when(page.getContentResource()).thenReturn(pageContent);

        when(pageContent.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(pageContent.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        GraphQLCategoryProvider categoryProvider = new GraphQLCategoryProvider(
            new MagentoGraphqlClientImpl(page.getContentResource(), null, null));

        // Test null categoryId
        Assert.assertTrue(categoryProvider.getChildCategories(null, 5).isEmpty());

        // Test category children found
        List<CategoryTree> categories = categoryProvider.getChildCategories("Mg==", 5);
        Assert.assertEquals(6, categories.size());
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
        String categoryIdentifier = "0";
        CategoryFilterInput categoryFilter = new CategoryFilterInput().setCategoryUid(new FilterEqualTypeInput().setEq(categoryIdentifier));
        QueryQuery.CategoryListArgumentsDefinition searchArgs = d -> d.filters(categoryFilter);

        QueryQuery topQuery = Operations.query(query -> query.categoryList(searchArgs, GraphQLCategoryProvider.defineCategoriesQuery(
            depth)));
        String queryString = topQuery.toString();

        // Trim "{categoryList(filters:{category_uid:{eq:"0"}}){" from the beginning and "}}" from the end of the string.
        queryString = queryString.substring(47, queryString.length() - 2);
        return queryString;
    }
}
