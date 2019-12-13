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

package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsRetriever.RelationType;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import io.wcm.testing.mock.aem.junit.AemContext;

public class RelationTypesDataSourceServletTest {

    @Rule
    public AemContext context = new AemContext();

    private RelationTypesDataSourceServlet servlet;

    @Before
    public void setUp() {
        servlet = new RelationTypesDataSourceServlet();
    }

    @Test
    public void testDataSource() throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());

        // Verify data source
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        Assert.assertNotNull(dataSource);

        AtomicInteger size = new AtomicInteger(0);
        dataSource.iterator().forEachRemaining(resource -> {
            ValueMapResource vmr = (ValueMapResource) resource;
            String value = vmr.getValueMap().get("value", String.class);
            String text = vmr.getValueMap().get("text", String.class);

            // Check that the value/text is a valid RelationType
            RelationType relationType = RelationType.valueOf(value);
            Assert.assertNotNull(relationType);
            Assert.assertEquals(relationType.toString(), value);
            Assert.assertEquals(relationType.getText(), text);

            size.incrementAndGet();
        });
        Assert.assertEquals(RelationType.values().length, size.get()); // Check that all RelationType enums have been collected
    }
}
