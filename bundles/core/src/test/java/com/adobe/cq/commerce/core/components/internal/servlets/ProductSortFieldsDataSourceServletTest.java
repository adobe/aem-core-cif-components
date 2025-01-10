/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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

import java.util.Iterator;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.granite.ui.components.ds.DataSource;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.components.internal.servlets.ProductSortFieldsDataSourceServlet.ICON_IMPLICIT_SORTFIELD;
import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ProductSortFieldsDataSourceServletTest {
    private static final String PAGE = "/content/pageA";
    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);
    private static final String PRODUCTLIST = "/content/pageA/jcr:content/root/responsivegrid/productlist";
    private static final String SEARCHRESULTS = "/content/pageA/jcr:content/root/responsivegrid/searchresults";
    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");
    @Mock
    CloseableHttpClient httpClient;
    private GraphqlClient graphqlClient;
    private MockSlingHttpServletRequest request;
    private ProductSortFieldsDataSourceServlet servlet;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver()) {
            @Override
            protected MockRequestPathInfo newMockRequestPathInfo() {
                return new MockRequestPathInfo() {
                    @Override
                    public Resource getSuffixResource() {
                        return context.currentResource().getResourceResolver().getResource(getSuffix());
                    }
                };
            }
        };
        context.currentResource(PAGE);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        graphqlClient = new GraphqlClientImpl();
        // Activate the GraphqlClientImpl with configuration
        context.registerInjectActivateService(graphqlClient, ImmutableMap.<String, Object>builder()
            .put("httpMethod", "POST")
            .put("url", "https://localhost")
            .build());
        Utils.setupHttpResponse("graphql/magento-graphql-sortkeys-result.json", httpClient, HttpStatus.SC_OK, "{products");
        context.registerAdapter(Resource.class, ComponentsConfiguration.class,
            (Function<Resource, ComponentsConfiguration>) input -> MOCK_CONFIGURATION_OBJECT);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient") != null ? graphqlClient : null);

        servlet = new ProductSortFieldsDataSourceServlet();
    }

    @Test
    public void testForProductList() {
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix(PRODUCTLIST);

        servlet.doGet(request, null);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        Iterator<Resource> iterator = dataSource.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "position", "Position", ICON_IMPLICIT_SORTFIELD);
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "name", "Product Name");
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "price", "Price");
        assertFalse(iterator.hasNext());

    }

    @Test
    public void testForSearchResults() {
        ((MockRequestPathInfo) request.getRequestPathInfo()).setSuffix(SEARCHRESULTS);

        servlet.doGet(request, null);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        Iterator<Resource> iterator = dataSource.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "position", "Position", ICON_IMPLICIT_SORTFIELD);
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "name", "Product Name");
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "price", "Price");
        assertTrue(iterator.hasNext());
        checkResource(iterator.next(), "relevance", "Relevance");
        assertFalse(iterator.hasNext());
    }

    private void checkResource(Resource resource, String value, String text) {
        checkResource(resource, value, text, null);
    }

    private void checkResource(Resource resource, String value, String text, String icon) {
        ValueMap vm = resource.getValueMap();
        assertNotNull(vm);
        assertEquals(value, vm.get("value", String.class));
        assertEquals(text, vm.get("text", String.class));
        if (icon != null) {
            assertEquals(icon, vm.get("icon", String.class));
        }
    }
}
