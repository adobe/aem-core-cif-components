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
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.osgi.framework.ServiceReference;

import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.search.internal.services.SearchFilterServiceImpl;
import com.adobe.cq.commerce.core.search.internal.services.SearchResultsServiceImpl;
import com.adobe.cq.commerce.core.testing.TestContext;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        aemContext.registerService(HttpClientBuilderFactory.class, new TestHttpClientBuilderFactory(httpClient));

        GraphqlClient graphqlClient = spy(new GraphqlClientImpl());
        Utils.registerGraphqlClient(aemContext, graphqlClient, null);
        aemContext.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);

        aemContext.registerService(BindingsValuesProvider.class, bindings -> bindings.put("wcmmode", wcmMode));
        when(wcmMode.isDisabled()).thenReturn(true);

        ContentPolicyManager contentPolicyManager = mock(ContentPolicyManager.class);
        aemContext.registerAdapter(ResourceResolver.class, ContentPolicyManager.class, contentPolicyManager);
        when(contentPolicyManager.getPolicy(any(), any())).thenReturn(contentPolicy);

        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-product-sku.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{url_key:{eq:\"beaumont-summit-kit\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{url_path:{eq:\"men/tops-men/jackets-men\"}}");
        Utils.setupHttpResponse(null, httpClient, HttpStatus.SC_NOT_FOUND, "{eq:\"does-not-exist\"}}");
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
        aemContext.currentPage("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/beaumont-summit-kit.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testReturns200ForCategory() throws ServletException, IOException {
        aemContext.currentPage("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/men/tops-men/jackets-men.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testReturns404ForMissingProduct() throws ServletException, IOException {
        aemContext.currentPage("/content/venia/us/en/products/product-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testReturns404ForMissingProductOnSpecificPageContentResource() throws ServletException, IOException {
        aemContext.currentResource("/content/venia/us/en/products/product-page/product-specific-page/jcr:content");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(contentModelFinder).findProductComponentModel(any(), any());
        verify(contentModelFinder, never()).findProductListComponentModel(any(), any());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testReturns404ForMissingCategory() throws ServletException, IOException {
        aemContext.currentPage("/content/venia/us/en/products/category-page");
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
        aemContext.currentPage("/content/venia/us/en/products/product-page");
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
        aemContext.currentPage("/content/venia/us/en/products/category-page");
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix("/does-not-exist.html");

        subject.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(contentModelFinder, never()).findProductComponentModel(any(), any());
        verify(contentModelFinder).findProductListComponentModel(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRegistersForForwardScope() throws Exception {
        ServiceReference<?>[] references = aemContext.bundleContext().getServiceReferences(Filter.class.getName(), null);
        assertNotNull(references);

        ServiceReference<?> filterReference = Arrays.stream(references)
            .filter(reference -> aemContext.bundleContext().getService(reference) == subject)
            .findFirst()
            .orElseThrow(() -> new AssertionError("CatalogPageNotFoundFilter service is not registered"));

        Object filterScopes = filterReference.getProperty(EngineConstants.SLING_FILTER_SCOPE);

        assertTrue(filterScopes instanceof String[]);
        assertTrue(Arrays.asList((String[]) filterScopes).contains(EngineConstants.FILTER_SCOPE_REQUEST));
        assertTrue(Arrays.asList((String[]) filterScopes).contains(EngineConstants.FILTER_SCOPE_FORWARD));
    }

    private static final class TestHttpClientBuilderFactory implements HttpClientBuilderFactory {
        private final CloseableHttpClient client;

        private TestHttpClientBuilderFactory(CloseableHttpClient client) {
            this.client = client;
        }

        @Override
        public HttpClientBuilder newBuilder() {
            return new HttpClientBuilder() {
                @Override
                public CloseableHttpClient build() {
                    return client;
                }
            };
        }
    }
}
