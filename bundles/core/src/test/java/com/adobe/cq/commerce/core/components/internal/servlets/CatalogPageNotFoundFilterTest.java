/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.CategoryRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CatalogPageNotFoundFilterTest {

    @Rule
    public final AemContext aemContext = TestContext.newAemContext("/context/jcr-content-breadcrumb.json");

    private final CatalogPageNotFoundFilter subject = new CatalogPageNotFoundFilter();
    private CommerceComponentModelFinder contentModelFinder;
    @Mock
    private FilterChain filterChain;
    @Mock
    private SightlyWCMMode wcmMode;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private GraphqlClient graphqlClient;
    private CloseableHttpClient httpClient;
    @Mock
    private ContentPolicy contentPolicy;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        request = aemContext.request();
        response = aemContext.response();

        // TODO: CIF-2469
        CommerceComponentModelFinder commerceModelFinder = new CommerceComponentModelFinder();
        Whitebox.setInternalState(commerceModelFinder, "modelFactory", aemContext.getService(ModelFactory.class));
        this.contentModelFinder = spy(commerceModelFinder);
        aemContext.registerService(CommerceComponentModelFinder.class, this.contentModelFinder);

        aemContext.registerService(CommerceExperienceFragmentsRetriever.class,
            mock(CommerceExperienceFragmentsRetriever.class));

        aemContext.registerInjectActivateService(new SearchFilterServiceImpl());
        aemContext.registerInjectActivateService(new SearchResultsServiceImpl());
        aemContext.registerInjectActivateService(subject);

        httpClient = mock(CloseableHttpClient.class);
        aemContext.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        graphqlClient = spy(new GraphqlClientImpl());
        Utils.registerGraphqlClient(aemContext, graphqlClient, null);
        aemContext.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);

        aemContext.registerService(BindingsValuesProvider.class, bindings -> bindings.put("wcmmode", wcmMode));
        when(wcmMode.isDisabled()).thenReturn(true);
        WCMMode.DISABLED.toRequest(request);

        ContentPolicyManager contentPolicyManager = mock(ContentPolicyManager.class);
        aemContext.registerAdapter(ResourceResolver.class, ContentPolicyManager.class, contentPolicyManager);
        when(contentPolicyManager.getPolicy(any(), any())).thenReturn(contentPolicy);

        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-product-sku.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{url_key:{eq:\"beaumont-summit-kit\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path:{eq:\"men/tops-men/jackets-men\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-empty-catalog-result.json", httpClient, HttpStatus.SC_OK,
            "{eq:\"does-not-exist\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK,
            "{__type(");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK,
            "{customAttributeMetadata");
        Utils.setupHttpResponse("graphql/magento-graphql-search-category-result-products.json", httpClient,
            HttpStatus.SC_OK, "pageSize:6");
    }

    /**
     * AEM mocks resolve {@code currentPage} to the page's {@code jcr:content} resource; the production filter must
     * still validate catalog pages when the request resource is the {@code cq:Page} node (REQUEST scope before
     * forward).
     */
    private void currentPageAsPageResource(String pagePath) {
        aemContext.currentPage(pagePath);
        Resource pageResource = aemContext.resourceResolver().getResource(pagePath);
        request.setResource(pageResource);
    }

    @Test
    public void testNoopOnNonCatalogPages() throws ServletException, IOException {
        aemContext.currentPage("/content/venia/us/en");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testNoopOnNonSlingRequestResponse() throws ServletException, IOException {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
    }

    @Test
    public void testReturns200ForProduct() throws ServletException, IOException {
        currentPageAsPageResource("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/beaumont-summit-kit.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testReturns200ForCategory() throws ServletException, IOException {
        currentPageAsPageResource("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/men/tops-men/jackets-men.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testJcrContentRequestPassesThrough() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/product-page/jcr:content");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testJcrContentDescendantRequestPassesThrough() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/product-page/jcr:content/root");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testForwardedJcrContentReturns404ForMissingProduct() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/product-page/jcr:content");
        request.setAttribute(SpecificPageFilterFactory.SPECIFIC_PAGE_FORWARD_ATTRIBUTE,
            "/content/venia/us/en/products/product-page/jcr:content");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testForwardedJcrContentReturns404ForMissingCategory() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/category-page/jcr:content");
        request.setAttribute(SpecificPageFilterFactory.SPECIFIC_PAGE_FORWARD_ATTRIBUTE,
            "/content/venia/us/en/products/category-page/jcr:content");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder).findProductListComponentModel(any(), any());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testForwardedJcrContentReturns200ForExistingProduct() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/product-page/jcr:content");
        request.setAttribute(SpecificPageFilterFactory.SPECIFIC_PAGE_FORWARD_ATTRIBUTE,
            "/content/venia/us/en/products/product-page/jcr:content");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/beaumont-summit-kit.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testForwardMarkerForDifferentPathPassesThrough() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/product-page/jcr:content/root");
        request.setAttribute(SpecificPageFilterFactory.SPECIFIC_PAGE_FORWARD_ATTRIBUTE,
            "/content/venia/us/en/products/product-page/jcr:content");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testReturns404ForMissingProduct() throws ServletException, IOException {
        currentPageAsPageResource("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testReturns404ForMissingCategory() throws ServletException, IOException {
        currentPageAsPageResource("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder).findProductListComponentModel(any(), any());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testReturns200ForMissingProductWithWcmModeNotDisabled() throws ServletException, IOException {
        when(wcmMode.isDisabled()).thenReturn(false);
        WCMMode.EDIT.toRequest(request);
        currentPageAsPageResource("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testReturns200ForMissingCategoryWithWcmModeNotDisabled() throws ServletException, IOException {
        when(wcmMode.isDisabled()).thenReturn(false);
        WCMMode.EDIT.toRequest(request);
        currentPageAsPageResource("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testProductTransportFailureReturns503() throws Exception {
        Product product = mockProduct(false, Collections.singletonList(transportError()), true);

        subject.doFilter(request, response, filterChain);

        assertEquals(503, response.getStatus());
        assertNull(response.getHeader("Retry-After"));
        assertFalse(response.getOutputAsString().contains("backend detail"));
        verify(filterChain, never()).doFilter(any(), any());
        org.mockito.InOrder order = inOrder(product);
        order.verify(product).getFound();
        order.verify(product).getProductRetriever();
    }

    @Test
    public void testProductGraphqlErrorKeeps404() throws Exception {
        mockProduct(false, Collections.singletonList(graphqlError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testFoundProductWithGraphqlErrorKeeps200() throws Exception {
        mockProduct(true, Collections.singletonList(graphqlError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testFoundProductWithTransportErrorReturns503() throws Exception {
        mockProduct(true, Collections.singletonList(transportError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(503, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testUninitializedProductErrorsKeep404() throws Exception {
        mockProduct(false, null, false);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testNullProductErrorsKeep404() throws Exception {
        mockProduct(false, null, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testNullProductRetrieverKeeps404() throws Exception {
        Product product = mockProduct(false, null, false);
        when(product.getProductRetriever()).thenReturn(null);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testMissingCategoryTransportFailureReturns503() throws Exception {
        mockCategory(false, Collections.singletonList(transportError()), new SearchResultsSetImpl(), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(503, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testFoundCategorySearchTransportFailureReturns503() throws Exception {
        SearchResultsSetImpl results = new SearchResultsSetImpl();
        results.setErrors(Collections.singletonList(transportError()));
        ProductList productList = mockCategory(true, Collections.emptyList(), results, true);

        subject.doFilter(request, response, filterChain);

        assertEquals(503, response.getStatus());
        verify(productList).getSearchResultsSet();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testFoundCategoryGraphqlSearchErrorKeeps200() throws Exception {
        SearchResultsSetImpl results = new SearchResultsSetImpl();
        results.setErrors(Collections.singletonList(graphqlError()));
        mockCategory(true, Collections.emptyList(), results, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testMissingCategoryGraphqlErrorKeeps404() throws Exception {
        mockCategory(false, Collections.singletonList(graphqlError()), new SearchResultsSetImpl(), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testUninitializedCategoryErrorsKeep404() throws Exception {
        mockCategory(false, null, new SearchResultsSetImpl(), false);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testNullCategoryAndSearchErrorsKeep200() throws Exception {
        SearchResultsSet results = mock(SearchResultsSet.class);
        when(results.getErrors()).thenReturn(null);
        mockCategory(true, null, results, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testNullCategoryRetrieverKeeps404() throws Exception {
        ProductList productList = mockCategory(false, null, null, false);
        when(productList.getCategoryRetriever()).thenReturn(null);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testKillSwitchKeepsProduct404WithoutReadingErrors() throws Exception {
        configure(false, 60);
        Product product = mockProduct(false, Collections.singletonList(transportError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
        assertNull(response.getHeader("Retry-After"));
        verify(product, never()).getProductRetriever();
    }

    @Test
    public void testKillSwitchDoesNotEagerlySearchExistingCategory() throws Exception {
        configure(false, 0);
        ProductList productList = mockCategory(true, Collections.singletonList(transportError()), null, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        verify(productList, never()).getProducts();
        verify(productList, never()).getSearchResultsSet();
    }

    @Test
    public void testKillSwitchKeepsMissingCategory404() throws Exception {
        configure(false, 0);
        ProductList productList = mockCategory(false, Collections.singletonList(transportError()), null, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
        verify(productList).getProducts();
        verify(productList, never()).getSearchResultsSet();
    }

    @Test
    public void testAuthorEditDoesNotInspectProductErrors() throws Exception {
        WCMMode.EDIT.toRequest(request);
        Product product = mockProduct(true, Collections.singletonList(transportError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        verify(product, never()).getProductRetriever();
    }

    @Test
    public void testAuthorPreviewDoesNotInspectProductErrors() throws Exception {
        WCMMode.PREVIEW.toRequest(request);
        Product product = mockProduct(true, Collections.singletonList(transportError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        verify(product, never()).getProductRetriever();
    }

    @Test
    public void testAuthorEditDoesNotEagerlySearchCategory() throws Exception {
        WCMMode.EDIT.toRequest(request);
        ProductList productList = mockCategory(true, Collections.singletonList(transportError()), null, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        verify(productList, never()).getProducts();
        verify(productList, never()).getSearchResultsSet();
    }

    @Test
    public void testAuthorPreviewDoesNotEagerlySearchCategory() throws Exception {
        WCMMode.PREVIEW.toRequest(request);
        ProductList productList = mockCategory(true, Collections.singletonList(transportError()), null, true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        verify(productList, never()).getProducts();
        verify(productList, never()).getSearchResultsSet();
    }

    @Test
    public void testRetryAfterSentWhenPositive() throws Exception {
        configure(true, 60);
        mockProduct(false, Collections.singletonList(transportError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(503, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    public void testNegativeRetryAfterNotSent() throws Exception {
        configure(true, -1);
        mockProduct(false, Collections.singletonList(transportError()), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(503, response.getStatus());
        assertNull(response.getHeader("Retry-After"));
    }

    @Test
    public void testRetryAfterNotSentForMissingProduct() throws Exception {
        configure(true, 60);
        mockProduct(false, Collections.emptyList(), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(404, response.getStatus());
        assertNull(response.getHeader("Retry-After"));
    }

    @Test
    public void testRetryAfterNotSentForHealthyProduct() throws Exception {
        configure(true, 60);
        mockProduct(true, Collections.emptyList(), true);
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Retry-After"));
    }

    @Test
    public void testFilterAndProductRenderingShareBackendResponses() throws Exception {
        currentPageAsPageResource("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/beaumont-summit-kit.html");
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        int callsAfterFilter = backendCalls().size();
        assertTrue(callsAfterFilter > 0);

        Product rendered = contentModelFinder.findProductComponentModel(request,
            aemContext.currentPage().getContentResource());
        assertTrue(rendered.getFound());

        assertEquals(callsAfterFilter, backendCalls().size());
    }

    @Test
    public void testFilterAndCategoryRenderingShareBackendResponses() throws Exception {
        currentPageAsPageResource("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/men/tops-men/jackets-men.html");
        subject.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
        List<GraphqlRequest> callsAfterFilter = backendCalls();
        assertTrue(callsAfterFilter.stream().anyMatch(call -> call.getQuery().startsWith("{products")));

        ProductList rendered = contentModelFinder.findProductListComponentModel(request,
            aemContext.currentPage().getContentResource());
        assertFalse(rendered.getProducts().isEmpty());

        assertEquals(callsAfterFilter.size(), backendCalls().size());
    }

    @Test
    public void testActualHttpFailureReturns503() throws Exception {
        Utils.setupHttpErrorResponse(httpClient, 503, "{eq:\"does-not-exist\"}}");
        currentPageAsPageResource("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");
        subject.doFilter(request, response, filterChain);
        assertEquals(503, response.getStatus());
    }

    @Test
    public void testActualCategoryHttpFailureReturns503() throws Exception {
        Utils.setupHttpErrorResponse(httpClient, 503, "{eq:\"does-not-exist\"}}");
        currentPageAsPageResource("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");
        subject.doFilter(request, response, filterChain);
        assertEquals(503, response.getStatus());
    }

    @Test
    public void testOneWarningWithPageAndErrorWithoutStackTrace() throws Exception {
        Error error = transportError();
        error.setMessage("backend detail\r\nconnection refused");
        mockProduct(false, Collections.singletonList(error), true);
        Logger logger = (Logger) LoggerFactory.getLogger(CatalogPageNotFoundFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            subject.doFilter(request, response, filterChain);
            List<ILoggingEvent> warnings = appender.list.stream().filter(event -> event.getLevel() == Level.WARN)
                .collect(java.util.stream.Collectors.toList());
            assertEquals(1, warnings.size());
            ILoggingEvent warning = warnings.get(0);
            assertTrue(warning.getFormattedMessage().contains("/content/venia/us/en/products/product-page"));
            assertTrue(warning.getFormattedMessage().contains("backend detail"));
            assertFalse(warning.getFormattedMessage().contains("\n"));
            assertNull(warning.getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private List<GraphqlRequest> backendCalls() {
        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);
        verify(graphqlClient, atLeastOnce()).execute(captor.capture(), any(), any(), any());
        return captor.getAllValues();
    }

    private void configure(boolean enabled, int retryAfter) {
        CatalogPageNotFoundFilterConfiguration config = mock(CatalogPageNotFoundFilterConfiguration.class);
        when(config.serviceUnavailableOnBackendError()).thenReturn(enabled);
        when(config.retryAfterSeconds()).thenReturn(retryAfter);
        subject.activate(aemContext.bundleContext(), config);
    }

    private Product mockProduct(boolean found, List<Error> errors, boolean initialized) {
        currentPageAsPageResource("/content/venia/us/en/products/product-page");
        AbstractProductRetriever retriever = new AbstractProductRetriever(mock(MagentoGraphqlClient.class)) {
            @Override
            protected ProductInterfaceQueryDefinition generateProductQuery() {
                return q -> q.sku();
            }
        };
        Product product = mock(Product.class);
        when(product.getFound()).thenAnswer(invocation -> {
            if (initialized) {
                Whitebox.setInternalState(retriever, "errors", errors);
            }
            return found;
        });
        when(product.getProductRetriever()).thenReturn(retriever);
        doReturn(product).when(contentModelFinder).findProductComponentModel(any(), any());
        return product;
    }

    private ProductList mockCategory(boolean found, List<Error> errors, SearchResultsSet results, boolean initialized) {
        currentPageAsPageResource("/content/venia/us/en/products/category-page");
        CategoryRetriever retriever = spy(new CategoryRetriever(mock(MagentoGraphqlClient.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            if (initialized) {
                Whitebox.setInternalState(retriever, "errors", errors);
            }
            return found ? new CategoryTree() : null;
        }).when(retriever).fetchCategory();
        ProductList productList = mock(ProductList.class);
        when(productList.getCategoryRetriever()).thenReturn(retriever);
        when(productList.getProducts()).thenReturn(Collections.emptyList());
        when(productList.getSearchResultsSet()).thenReturn(results);
        doReturn(productList).when(contentModelFinder).findProductListComponentModel(any(), any());
        return productList;
    }

    private static Error transportError() {
        Error error = new Error();
        error.setCategory(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY);
        error.setMessage("backend detail");
        return error;
    }

    private static Error graphqlError() {
        Error error = new Error();
        error.setCategory("graphql-input");
        error.setMessage("currentPage value 999 is greater than the available pages");
        return error;
    }
}
