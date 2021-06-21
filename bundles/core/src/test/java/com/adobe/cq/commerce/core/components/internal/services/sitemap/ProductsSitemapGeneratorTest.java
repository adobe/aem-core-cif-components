package com.adobe.cq.commerce.core.components.internal.services.sitemap;

import java.io.IOException;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.common.SitemapLinkExternalizer;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.sitemap.SitemapProductFilter;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductsSitemapGeneratorTest {

    @Rule
    public final AemContext aemContext = new AemContext();

    private final ProductsSitemapGenerator subject = new ProductsSitemapGenerator();
    private final UrlProviderImpl urlProvider = new UrlProviderImpl();
    private final GraphqlClient graphqlClient = Utils.setupGraphqlClient();

    @Mock
    private SitemapLinkExternalizer externalizer;
    @Mock
    private SitemapProductFilter productFilter;
    @Mock
    private SitemapGenerator.GenerationContext context;
    @Mock
    private Sitemap sitemap;

    private Page homePage;
    private Page productPage;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        homePage = aemContext.create().page(
            "/content/site/en",
            "homepage-template",
            ImmutableMap.of("cq:cifProductPage", "/content/site/en/product-page"));
        productPage = aemContext.create().page(homePage.getPath() + "/product-page");

        aemContext.registerService(SitemapProductFilter.class, productFilter);
        aemContext.registerService(SitemapLinkExternalizer.class, externalizer);
        aemContext.registerInjectActivateService(urlProvider);
        aemContext.registerInjectActivateService(subject, "pageSize", 2);

        aemContext.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        aemContext.registerAdapter(Resource.class, ComponentsConfiguration.class, ComponentsConfiguration.EMPTY);

        when(productFilter.shouldInclude(any(), any())).thenReturn(Boolean.TRUE);
        when(externalizer.externalize(any())).then(inv -> ((Resource) inv.getArguments()[0]).getPath());
        when(context.getProperty(eq(ProductsSitemapGenerator.PN_NEXT_PRODUCT), anyInt()))
            .then(inv -> inv.getArguments()[1]);
        when(context.getProperty(eq(ProductsSitemapGenerator.PN_NEXT_PAGE), anyInt()))
            .then(inv -> inv.getArguments()[1]);

        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-product-page-1.json",
            "{products(search:\"\",pageSize:2,currentPage:1)");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-product-page-2.json",
            "{products(search:\"\",pageSize:2,currentPage:2)");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-product-page-3.json",
            "{products(search:\"\",pageSize:2,currentPage:3)");
        Utils.addHttpResponseFrom(graphqlClient,
            "graphql/sitemap/magento-graphql-sitemap-product-page-error.json",
            "{products(search:\"\",pageSize:2,currentPage:999)");
    }

    @Test(expected = SitemapException.class)
    public void testAnyErrorRethrown() throws SitemapException {
        // given
        when(context.getProperty(eq(ProductsSitemapGenerator.PN_NEXT_PAGE), anyInt())).thenReturn(999);
        // when
        subject.generate(productPage.adaptTo(Resource.class), "<default>", sitemap, context);
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
        Set<String> names = subject.getNames(productPage.adaptTo(Resource.class));
        // then
        assertEquals("names' size expected to be 1", 1, names.size());
        assertTrue("names expected to contain <default>", names.contains("<default>"));
    }

    @Test
    public void testAllProductsAdded() throws SitemapException {
        // given
        ArgumentCaptor<String> locations = ArgumentCaptor.forClass(String.class);
        // when
        subject.generate(productPage.adaptTo(Resource.class), "<default>", sitemap, context);
        // then
        verify(sitemap, atLeastOnce()).addUrl(locations.capture());
        assertEquals("5 locations added", 5, locations.getAllValues().size());
    }

    @Test
    public void testAllProductsAddedAfterResume() throws SitemapException {
        // given
        ArgumentCaptor<String> locations = ArgumentCaptor.forClass(String.class);
        when(context.getProperty(eq(ProductsSitemapGenerator.PN_NEXT_PAGE), anyInt())).thenReturn(2);
        when(context.getProperty(eq(ProductsSitemapGenerator.PN_NEXT_PRODUCT), anyInt())).thenReturn(1);

        // when
        subject.generate(productPage.adaptTo(Resource.class), "<default>", sitemap, context);
        // then
        verify(sitemap, atLeastOnce()).addUrl(locations.capture());
        assertEquals("2 locations added", 2, locations.getAllValues().size());
    }

    @Test
    public void testOnlyProductsAllowedByFilterAdded() throws SitemapException {
        // given
        ArgumentCaptor<String> locations = ArgumentCaptor.forClass(String.class);
        when(productFilter.shouldInclude(any(), any())).then(inv -> {
            ProductInterface product = (ProductInterface) inv.getArguments()[1];
            return product.getSku().equals("P04");
        });

        // when
        subject.generate(productPage.adaptTo(Resource.class), "<default>", sitemap, context);
        // then
        verify(sitemap, atLeastOnce()).addUrl(locations.capture());
        assertEquals("1 locations added", 1, locations.getAllValues().size());
    }
}
