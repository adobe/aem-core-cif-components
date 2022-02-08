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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.granite.ui.components.ds.DataSource;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.wcm.testing.mock.aem.junit.AemContext;

public class CategoryPageUrlFormatsServletTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private Object[] expectedValues;
    private CategoryPageUrlFormatsServlet datasource;

    @Before
    public void setUp() {
        datasource = context.registerInjectActivateService(new CategoryPageUrlFormatsServlet());
        List<String> expectedValuesList = new ArrayList<>();
        expectedValuesList.add("");
        UrlProviderImpl.DEFAULT_CATEGORY_URL_FORMATS.keySet().forEach(f -> expectedValuesList.add(f.replace(
            "#", "\\#")));
        expectedValuesList.add(
            "com.adobe.cq.commerce.core.components.internal.servlets.CategoryPageUrlFormatsServletTest$CustomUrlFormat");
        expectedValues = expectedValuesList.toArray();
    }

    @Test
    public void testDataSource() throws IllegalAccessException {
        Resource dummyResource = context.create().resource("/not/needed", ImmutableMap.of("sling:resourceType",
            "cif/gui/datasource/categoryurlformats"));
        context.request().setResource(dummyResource);
        FieldUtils.writeField(datasource, "categoryPageUrlFormats", Arrays.asList(new CustomUrlFormat()), true);

        datasource.doGet(context.request(), context.response());
        DataSource ds = (DataSource) context.request().getAttribute(DataSource.class.getName());

        Assert.assertNotNull("Datasource is not null", ds);

        List<String> formats = new ArrayList<>();
        ds.iterator().forEachRemaining(r -> {
            formats.add(r.getValueMap().get("value", String.class));
        });

        Assert.assertArrayEquals("Data source has the correct data", expectedValues,
            formats.toArray());
    }

    private static class CustomUrlFormat implements UrlFormat {
        @Override
        public String format(Map<String, String> parameters) {
            return "";
        }

        @Override
        public Map<String, String> parse(RequestPathInfo requestPathInfo, RequestParameterMap parameterMap) {
            return new HashMap<>();
        }

        @Override
        public Set<String> getParameterNames() {
            return ImmutableSet.of("uid", "sku");
        }
    }

}
