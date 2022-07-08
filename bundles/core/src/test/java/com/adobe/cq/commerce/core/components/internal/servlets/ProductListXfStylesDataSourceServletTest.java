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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductListXfStylesDataSourceServletTest {

    private ProductListXfStylesDataSourceServlet dataSourceServlet;

    @Rule
    public final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).build();

    ContentPolicyManager policyManager;
    ContentPolicy policy;

    @Before
    public void setUp() {
        dataSourceServlet = new ProductListXfStylesDataSourceServlet();
        context.load().json("/context/jcr-conf.json", "/conf/testing");
        context.load().json("/context/jcr-content.json", "/content");
        context.request().setAttribute(Value.CONTENTPATH_ATTRIBUTE,
            "/content/pageA/jcr:content/root/responsivegrid/productlist_with_xf/fragments/item0");
        policyManager = mock(ContentPolicyManager.class);
        policy = mock(ContentPolicy.class);

        context.registerAdapter(ResourceResolver.class, ContentPolicyManager.class, policyManager);
    }

    @Test
    public void testDataSource() throws ServletException, IOException {
        when(policy.getPath()).thenReturn("/conf/testing/settings/wcm/policies/testing");
        when(policyManager.getPolicy((Resource) any())).thenReturn(policy);
        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<ProductListXfStylesDataSourceServlet.XfStyleResource> styles = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            styles.add((ProductListXfStylesDataSourceServlet.XfStyleResource) resource);
        });

        // Verify values
        assertEquals(1, styles.size());
        ProductListXfStylesDataSourceServlet.XfStyleResource first = styles.get(0);
        assertEquals("marketing-content__row-2", first.getValue());
        assertEquals("Second Row", first.getText());
    }

    @Test
    public void testDataSourceWithNoPolicy() throws ServletException, IOException {
        when(policyManager.getPolicy((Resource) any())).thenReturn(null);
        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<ProductListXfStylesDataSourceServlet.XfStyleResource> styles = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            styles.add((ProductListXfStylesDataSourceServlet.XfStyleResource) resource);
        });

        // Verify values
        assertEquals(0, styles.size());
    }

    @Test
    public void testDataSourceWithNoValues() throws ServletException, IOException {
        when(policy.getPath()).thenReturn("/conf/testing/settings/wcm/policies/empty");
        when(policyManager.getPolicy((Resource) any())).thenReturn(policy);

        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<ProductListXfStylesDataSourceServlet.XfStyleResource> styles = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            styles.add((ProductListXfStylesDataSourceServlet.XfStyleResource) resource);
        });

        // Verify values
        assertEquals(0, styles.size());
    }
}
