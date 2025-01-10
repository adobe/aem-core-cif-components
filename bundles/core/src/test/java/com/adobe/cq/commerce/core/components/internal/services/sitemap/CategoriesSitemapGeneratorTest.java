/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.sitemap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.spi.common.SitemapLinkExternalizer;
import org.apache.sling.sitemap.spi.generator.SitemapGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.sitemap.SitemapCategoryFilter;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CategoriesSitemapGeneratorTest {

    @Rule
    public final AemContext aemContext = TestContext.newAemContext();

    private final CategoriesSitemapGenerator subject = new CategoriesSitemapGenerator();
    private final GraphqlClient graphqlClient = new GraphqlClientImpl();

    @Mock
    private SitemapLinkExternalizer externalizer;
    @Mock
    private SitemapCategoryFilter categoryFilter;
    @Mock
    private SitemapGenerator.Context context;
    @Mock
    private Sitemap sitemap;

    private Page homePage;
    private Page categoryPage;
    private ValueMap configuration = new ValueMapDecorator(new HashMap<>());

    @Before
    public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);

        configuration.put(CategoriesSitemapGenerator.PN_MAGENTO_ROOT_CATEGORY_ID, "UID2");

        homePage = aemContext.create().page(
            "/content/site/en",
            "homepage-template",
            ImmutableMap.of(
                "navRoot", true,
                "cq:cifCategoryPage", "/content/site/en/category-page"));
        categoryPage = aemContext.create().page(homePage.getPath() + "/category-page");

        aemContext.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory());

        // Mock and set the protected 'client' field
        Utils.setClientField(graphqlClient, mock(HttpClient.class));

        // Activate the GraphqlClientImpl with configuration
        aemContext.registerInjectActivateService(graphqlClient, ImmutableMap.<String, Object>builder()
            .put("httpMethod", "POST")
            .put("url", "https://localhost")
            .build());
        aemContext.registerService(SitemapCategoryFilter.class, categoryFilter);
        aemContext.registerService(SitemapLinkExternalizer.class, externalizer);
        aemContext.registerInjectActivateService(new SitemapLinkExternalizerProvider());
        aemContext.registerInjectActivateService(subject);

        aemContext.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);

        when(context.getProperty(eq(CategoriesSitemapGenerator.PN_PENDING_CATEGORIES), any(String[].class)))
            .then(inv -> inv.getArguments()[1]);
        when(categoryFilter.shouldInclude(any(), any())).thenReturn(Boolean.TRUE);
        when(externalizer.externalize(any())).then(inv -> ((Resource) inv.getArguments()[0]).getPath());

        // mock category tree, 3 1st level, for each 2 2nd level
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-2.json",
            "{categories(filters:{category_uid:{eq:\"UID2\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-3.json",
            "{categories(filters:{category_uid:{eq:\"UID3\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-31.json",
            "{categories(filters:{category_uid:{eq:\"UID31\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-32.json",
            "{categories(filters:{category_uid:{eq:\"UID32\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-4.json",
            "{categories(filters:{category_uid:{eq:\"UID4\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-41.json",
            "{categories(filters:{category_uid:{eq:\"UID41\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-42.json",
            "{categories(filters:{category_uid:{eq:\"UID42\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-5.json",
            "{categories(filters:{category_uid:{eq:\"UID5\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-51.json",
            "{categories(filters:{category_uid:{eq:\"UID51\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-tree-52.json",
            "{categories(filters:{category_uid:{eq:\"UID52\"");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-category-error.json",
            "{categories(filters:{category_uid:{eq:\"ERROR\"");
    }

    @Test
    public void testNamesEmptyForContentPage() {
        // given
        Page page = aemContext.create().page(homePage.getPath() + "/content-page");
        // when
        Set<String> names = subject.getNames(page.adaptTo(Resource.class));
        // then
        assertTrue("names expected to be empty", names.isEmpty());
    }

    @Test
    public void testNamesNotEmptyForCategoryPage() {
        // given
        // when
        Set<String> names = subject.getNames(categoryPage.adaptTo(Resource.class));
        // then
        assertEquals("names' size expected to be 1", 1, names.size());
        assertTrue("names expected to contain <default>", names.contains("<default>"));
    }

    @Test(expected = SitemapException.class)
    public void testAnyErrorRethrown() throws SitemapException {
        // given
        configuration.put(CategoriesSitemapGenerator.PN_MAGENTO_ROOT_CATEGORY_ID, "ERROR");
        aemContext.registerAdapter(Resource.class, ComponentsConfiguration.class, new ComponentsConfiguration(configuration));

        // when
        subject.generate(categoryPage.adaptTo(Resource.class), "<default>", sitemap, context);
    }

    @Test
    public void testSitemapContainsAllButRootCategory() throws SitemapException {
        // given
        ArgumentCaptor<String> locations = ArgumentCaptor.forClass(String.class);
        aemContext.registerAdapter(Resource.class, ComponentsConfiguration.class, new ComponentsConfiguration(configuration));

        // when
        subject.generate(categoryPage.adaptTo(Resource.class), "<default>", sitemap, context);

        // then
        verify(sitemap, atLeastOnce()).addUrl(locations.capture());
        assertEquals("9 locations expected", 9, locations.getAllValues().size());
    }

    @Test
    public void testSitemapContainsAllAllowedByFilter() throws SitemapException {
        // given
        ArgumentCaptor<String> locations = ArgumentCaptor.forClass(String.class);
        aemContext.registerAdapter(Resource.class, ComponentsConfiguration.class, new ComponentsConfiguration(configuration));
        when(categoryFilter.shouldInclude(any(), any())).then(inv -> {
            CategoryTree categoryTree = (CategoryTree) inv.getArguments()[1];
            // 1st level only is 2,3,4,5
            return categoryTree.getId() < 10;
        });

        // when
        subject.generate(categoryPage.adaptTo(Resource.class), "<default>", sitemap, context);

        // then
        verify(sitemap, atLeastOnce()).addUrl(locations.capture());
        assertEquals("3 locations expected", 3, locations.getAllValues().size());
    }

    /**
     * In order to keep track on the category-tree traversal state a list of pending category ids is maintained in the context of the
     * sitemap generation job. This test ensures that this list is growing only up to the minimum of required information, that is, for
     * each level visited the list of category ids.
     * <p>
     * For example In the following tree:
     * - 2: [3,4,5]
     * - 3: [31, 32]
     * - 4: [41, 42]
     * - 5: [51, 52]
     * A traversal may visit 2,3,31,32,4,41,42,5,51,52. With 3 categories in the first level and up to 2 in the 2nd level a maximum of 4
     * categories must be kept to restore the traversal state: len(1st-level) - 1 + max(len(2n-level)).
     *
     * @throws SitemapException
     */
    @Test
    public void testPendingCategoryIdsDoesntGrowLarge() throws SitemapException {
        // given
        ArgumentCaptor<String[]> pendingCategoryIdsState = ArgumentCaptor.forClass(String[].class);
        aemContext.registerAdapter(Resource.class, ComponentsConfiguration.class, new ComponentsConfiguration(configuration));

        // when
        subject.generate(categoryPage.adaptTo(Resource.class), "<default>", sitemap, context);

        // then
        verify(context, atLeastOnce()).setProperty(eq(CategoriesSitemapGenerator.PN_PENDING_CATEGORIES), pendingCategoryIdsState.capture());
        int maxPendingCategoryIds = pendingCategoryIdsState.getAllValues().stream()
            .mapToInt(array -> array.length).max().orElse(Integer.MAX_VALUE);
        assertEquals("max of pending category ids expected to not exceed 4", 4, maxPendingCategoryIds);
    }

    @Test
    public void testResumeWithCategoryIds() throws SitemapException {
        // given
        ArgumentCaptor<String> locations = ArgumentCaptor.forClass(String.class);
        aemContext.registerAdapter(Resource.class, ComponentsConfiguration.class, new ComponentsConfiguration(configuration));
        when(context.getProperty(eq(CategoriesSitemapGenerator.PN_PENDING_CATEGORIES), any(String[].class))).thenReturn(
            new String[] { "UID42", "UID5" });

        // when
        subject.generate(categoryPage.adaptTo(Resource.class), "<default>", sitemap, context);

        // then
        verify(sitemap, atLeastOnce()).addUrl(locations.capture());
        assertEquals("4 locations expected", 4, locations.getAllValues().size());
    }
}
