/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.datalayer;

import java.io.IOException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.datalayer.CategoryData;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.wcm.core.components.models.Component;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataLayerComponentTest {

    private static final String CONTENT_PATH = "/context/jcr-content-datalayer.json";
    private static final String PAGE = "/content/venia/us/en";
    private static final String RESOURCE_RELATIVE_PATH = "/jcr:content/root/responsivegrid/test";
    private static final String ITEM_RESOURCE_RELATIVE_PATH = "/jcr:content/root/responsivegrid/test-item";

    private TestSimpleComponent testComponent;
    private Resource testResource;
    private ConfigurationBuilder mockConfigBuilder;

    @Rule
    public final AemContext context = createContext();

    private static AemContext createContext() {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(DataLayerComponentTest.CONTENT_PATH, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    static class TestSimpleComponent extends DataLayerComponent implements Component {
        public TestSimpleComponent(Resource resource) {
            this.resource = resource;
        }
    }

    static class TestProductComponent extends DataLayerComponent implements Component {

        public TestProductComponent(Resource resource) {
            this.resource = resource;
        }

        @Override
        protected ComponentData getComponentData() {
            return new ProductDataImpl(this, resource);
        }

        @Override
        public String getDataLayerTitle() {
            return "Test title";
        }

        @Override
        public String getDataLayerSKU() {
            return "test-sku";
        }

        @Override
        public Double getDataLayerPrice() {
            return 10.2;
        }

        @Override
        public String getDataLayerCurrency() {
            return "USD";
        }
    }

    static class TestListItemComponent extends DataLayerListItem implements Component {

        protected TestListItemComponent(String parentId, Resource resource) {
            super(parentId, resource);
        }

        @Override
        protected ComponentData getComponentData() {
            return new CategoryListDataImpl(this, resource);
        }

        @Override
        public String getDataLayerTitle() {
            return "Test item";
        }

        @Override
        public CategoryData[] getDataLayerCategories() {
            return new CategoryListDataImpl.CategoryDataImpl[] {
                new CategoryListDataImpl.CategoryDataImpl("cat1", "Category 1", "/url/to/img1"),
                new CategoryListDataImpl.CategoryDataImpl("cat2", "Category 2", "/url/to/img2")
            };
        }
    }

    @Before
    public void setup() {
        mockConfigBuilder = Utils.getDataLayerConfig(true);

        context.currentPage(PAGE);
        context.currentResource(PAGE + RESOURCE_RELATIVE_PATH);
        testResource = Mockito.spy(context.resourceResolver().getResource(PAGE + RESOURCE_RELATIVE_PATH));
        Mockito.when(testResource.adaptTo(ConfigurationBuilder.class)).thenReturn(mockConfigBuilder);

        testComponent = new TestSimpleComponent(testResource);
    }

    @Test
    public void testConfigDisabled() {
        Mockito.when(mockConfigBuilder.asValueMap()).thenReturn(new ValueMapDecorator(ImmutableMap.of("enabled", false)));
        assertNull(testComponent.getData());

        assertEquals(testComponent.getComponentData().getClass(), ComponentDataImpl.class);
    }

    @Test
    public void testJsonRender() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        TestProductComponent testProductComponent = new TestProductComponent(testResource);
        String expected = Utils.getResource("results/result-datalayer-test-resource.json");
        String jsonResult = testProductComponent.getData().getJson();
        assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

    @Test
    public void testListItemJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        context.currentResource(PAGE + ITEM_RESOURCE_RELATIVE_PATH);
        Resource testItemResource = Mockito.spy(context.resourceResolver().getResource(PAGE + ITEM_RESOURCE_RELATIVE_PATH));
        Mockito.when(testItemResource.adaptTo(ConfigurationBuilder.class)).thenReturn(mockConfigBuilder);

        TestListItemComponent testItemComponent = new TestListItemComponent(testComponent.getId(), testItemResource);
        String expected = Utils.getResource("results/result-datalayer-item-component.json");
        String jsonResult = testItemComponent.getData().getJson();
        assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }
}
