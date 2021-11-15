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
package com.adobe.cq.commerce.extensions.recommendations.internal.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.extensions.recommendations.testing.TestContext;
import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AllowedRecTypesDataSourceServletTest {

    private AllowedRecTypesDataSourceServlet dataSourceServlet;

    @Rule
    public final AemContext context = TestContext.newAemContext("/context/datasource-content.json");

    @Before
    public void setUp() {
        dataSourceServlet = new AllowedRecTypesDataSourceServlet();
        context.request().setAttribute(Value.CONTENTPATH_ATTRIBUTE, "/content/landingPage/jcr:content/par/product-recs");
    }

    @Test
    public void testDataSource() throws ServletException, IOException {
        // Define policy mapping
        context.contentPolicyMapping("core/cif/extensions/product-recs/components/productrecommendations/v1/productrecommendations",
            AllowedRecTypesDataSourceServlet.PN_ALLOWED_TYPES, new String[] { "most-viewed", "recently-viewed" });

        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<AllowedRecTypesDataSourceServlet.RecTypeResource> allowedTypes = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            allowedTypes.add((AllowedRecTypesDataSourceServlet.RecTypeResource) resource);
        });

        // Verify values
        assertEquals(2, allowedTypes.size());
        AllowedRecTypesDataSourceServlet.RecTypeResource first = allowedTypes.get(0);
        assertEquals("most-viewed", first.getValue());
        assertEquals("Most viewed", first.getText());

        AllowedRecTypesDataSourceServlet.RecTypeResource second = allowedTypes.get(1);
        assertEquals("recently-viewed", second.getValue());
        assertEquals("Recently viewed", second.getText());
    }

    @Test
    public void testDataSourceWithInvalidValue() throws ServletException, IOException {
        // Define policy mapping with invalid value
        context.contentPolicyMapping("core/cif/extensions/product-recs/components/productrecommendations/v1/productrecommendations",
            AllowedRecTypesDataSourceServlet.PN_ALLOWED_TYPES, new String[] { "super-custom", "recently-viewed" });

        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<AllowedRecTypesDataSourceServlet.RecTypeResource> allowedTypes = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            allowedTypes.add((AllowedRecTypesDataSourceServlet.RecTypeResource) resource);
        });

        // Verify values
        assertEquals(1, allowedTypes.size());
        AllowedRecTypesDataSourceServlet.RecTypeResource first = allowedTypes.get(0);
        assertEquals("recently-viewed", first.getValue());
        assertEquals("Recently viewed", first.getText());
    }

    @Test
    public void testDataSourceWithNoPolicy() throws ServletException, IOException {
        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<AllowedRecTypesDataSourceServlet.RecTypeResource> allowedTypes = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            allowedTypes.add((AllowedRecTypesDataSourceServlet.RecTypeResource) resource);
        });

        // Verify values
        assertEquals(0, allowedTypes.size());
    }

    @Test
    public void testDataSourceWithNoValues() throws ServletException, IOException {
        // Define policy mapping with no types
        context.contentPolicyMapping("core/cif/extensions/product-recs/components/productrecommendations/v1/productrecommendations",
            "something", "value");

        // Call datasource servlet
        dataSourceServlet.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);

        List<AllowedRecTypesDataSourceServlet.RecTypeResource> allowedTypes = new ArrayList<>();
        dataSource.iterator().forEachRemaining(resource -> {
            allowedTypes.add((AllowedRecTypesDataSourceServlet.RecTypeResource) resource);
        });

        // Verify values
        assertEquals(0, allowedTypes.size());
    }

    @Test
    public void testAdaptsToValueMap() {
        AllowedRecTypesDataSourceServlet.RecType recType = AllowedRecTypesDataSourceServlet.RecType.BOUGHT_BOUGHT;
        AllowedRecTypesDataSourceServlet.RecTypeResource resource = new AllowedRecTypesDataSourceServlet.RecTypeResource(recType, context
            .resourceResolver());

        ValueMap valueMap = resource.adaptTo(ValueMap.class);
        assertEquals("bought-bought", valueMap.get("value", String.class));
        assertEquals("Bought this, bought that", valueMap.get("text", String.class));
    }

}
