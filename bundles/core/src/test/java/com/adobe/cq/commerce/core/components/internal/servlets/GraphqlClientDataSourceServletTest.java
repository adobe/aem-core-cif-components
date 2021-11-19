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
package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GraphqlClientDataSourceServletTest {

    @Rule
    public AemContext context = new AemContext();

    private GraphqlClientDataSourceServlet servlet;

    private ExpressionResolver mockExpressionResolver;
    private ResourceBundleProvider resourceBundleProvider;
    private ResourceBundle translations = new ListResourceBundle() {
        // empty
        @Override
        protected Object[][] getContents() {
            return new Object[0][];
        }
    };

    @Before
    public void setUp() {
        servlet = new GraphqlClientDataSourceServlet();
        mockExpressionResolver = mock(ExpressionResolver.class);
        context.registerService(ExpressionResolver.class, mockExpressionResolver);
        context.registerInjectActivateService(servlet);
        resourceBundleProvider = mock(ResourceBundleProvider.class);
        when(resourceBundleProvider.getResourceBundle(any())).then(inv -> translations);
        when(resourceBundleProvider.getResourceBundle(any(), any())).then(inv -> translations);
        context.registerService(ResourceBundleProvider.class, resourceBundleProvider);
    }

    @Test
    public void testDataSource() {
        // Stub out getGraphqlClients call
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(new GraphqlClientDataSourceServlet.GraphqlClientResource("my-name", "my-value", null));
        GraphqlClientDataSourceServlet spyServlet = spy(servlet);
        Mockito.doReturn(resources).when(spyServlet).getGraphqlClients(any());

        // Test doGet
        spyServlet.doGet(context.request(), context.response());

        // Verify data source
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        Assert.assertNotNull(dataSource);

        AtomicInteger size = new AtomicInteger(0);
        dataSource.iterator().forEachRemaining(resource -> {
            GraphqlClientDataSourceServlet.GraphqlClientResource client = (GraphqlClientDataSourceServlet.GraphqlClientResource) resource;
            Assert.assertEquals("my-name", client.getText());
            Assert.assertEquals("my-value", client.getValue());
            Assert.assertFalse(client.getSelected());
            size.incrementAndGet();
        });
        Assert.assertEquals(1, size.get());
    }

    @Test
    public void testGetGraphqlClients() {
        // Stub i18n
        translations = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {
                    { "Inherit ((Inherit property))", "inherit-translated" }
                };
            }
        };

        // Add fake identifiers
        Set<String> identifiers = new HashSet<>();
        identifiers.add("my-identifier");
        Whitebox.setInternalState(servlet, "identifiers", identifiers);

        Resource mockResource = Mockito.mock(Resource.class);
        Map<String, Object> datasourceProps = new HashMap<String, Object>();
        datasourceProps.put("showEmptyOption", true);
        Resource datasourceDefinition = new MockResource("/some/random/path", datasourceProps, context.resourceResolver());
        Mockito.when(mockResource.getChild(Config.DATASOURCE)).thenReturn(datasourceDefinition);

        Mockito.when(mockExpressionResolver.resolve(Mockito.any(String.class), Mockito.any(Locale.class), Mockito.any(Class.class), Mockito
            .any(SlingHttpServletRequest.class))).thenReturn(true);

        context.request().setResource(mockResource);

        // Call method
        List<Resource> resources = servlet.getGraphqlClients(context.request());

        // Verify list
        Assert.assertEquals(2, resources.size());

        // Verify inherit entry
        GraphqlClientDataSourceServlet.GraphqlClientResource inherit = (GraphqlClientDataSourceServlet.GraphqlClientResource) resources.get(
            0);
        Assert.assertEquals("inherit-translated", inherit.getText());
        Assert.assertEquals("", inherit.getValue());
        Assert.assertFalse(inherit.getSelected());

        // Verify actual client
        GraphqlClientDataSourceServlet.GraphqlClientResource client = (GraphqlClientDataSourceServlet.GraphqlClientResource) resources.get(
            1);
        Assert.assertEquals("my-identifier", client.getText());
        Assert.assertEquals("my-identifier", client.getValue());
        Assert.assertFalse(client.getSelected());
    }

    @Test
    public void testGetGraphqlClientsNoEmptyOption() {
        // Add fake identifiers
        Set<String> identifiers = new HashSet<>();
        identifiers.add("my-identifier");
        Whitebox.setInternalState(servlet, "identifiers", identifiers);

        Resource mockResource = Mockito.mock(Resource.class);
        Map<String, Object> datasourceProps = new HashMap<String, Object>();
        datasourceProps.put("showEmptyOption", false);
        Resource datasourceDefinition = new MockResource("/some/random/path", datasourceProps, context.resourceResolver());

        Mockito.when(mockResource.getChild(Config.DATASOURCE)).thenReturn(datasourceDefinition);

        context.request().setResource(mockResource);

        // Call method
        List<Resource> resources = servlet.getGraphqlClients(context.request());

        // Verify list
        Assert.assertEquals(1, resources.size());

        // Verify actual client
        GraphqlClientDataSourceServlet.GraphqlClientResource client = (GraphqlClientDataSourceServlet.GraphqlClientResource) resources.get(
            0);
        Assert.assertEquals("my-identifier", client.getText());
        Assert.assertEquals("my-identifier", client.getValue());
        Assert.assertFalse(client.getSelected());
    }

    @Test
    public void testGraphqlClientResourceAdapt() {
        // Create resource
        GraphqlClientDataSourceServlet.GraphqlClientResource resource = new GraphqlClientDataSourceServlet.GraphqlClientResource("my-name",
            "my-value", null);

        // Adapt to ValueMap
        ValueMap map = resource.adaptTo(ValueMap.class);

        // Verify ValueMap
        Assert.assertEquals("my-name", map.get(GraphqlClientDataSourceServlet.GraphqlClientResource.PN_TEXT));
        Assert.assertEquals("my-value", map.get(GraphqlClientDataSourceServlet.GraphqlClientResource.PN_VALUE));
    }

    @Test
    public void testBindGraphqlClient() {
        GraphqlClient mockClient = mock(GraphqlClient.class);
        Mockito.doReturn("my-identifier").when(mockClient).getIdentifier();

        servlet.bindGraphqlClient(mockClient, Collections.emptyMap());

        Set<String> identifiers = (Set<String>) Whitebox.getInternalState(servlet, "identifiers");
        Assert.assertEquals(1, identifiers.size());
        Assert.assertTrue(identifiers.contains("my-identifier"));
    }

    @Test
    public void testUnbindGraphqlClient() {
        GraphqlClient mockClient = mock(GraphqlClient.class);
        Mockito.doReturn("my-identifier").when(mockClient).getIdentifier();

        Set<String> identifiers = new HashSet<>(Arrays.asList("my-identifier", "another-identifier"));
        Whitebox.setInternalState(servlet, "identifiers", identifiers);

        servlet.unbindGraphqlClient(mockClient, Collections.emptyMap());

        Assert.assertEquals(1, identifiers.size());
        Assert.assertTrue(identifiers.contains("another-identifier"));
    }
}
