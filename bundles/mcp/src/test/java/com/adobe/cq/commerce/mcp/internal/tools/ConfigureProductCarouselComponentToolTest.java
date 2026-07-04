/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.mcp.internal.tools;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigureProductCarouselComponentToolTest {
    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext ctxForResolver() {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    @Test
    public void bindsProductSelectionWithCombinedSkuNormalization() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel").commit();

        ConfigureProductCarouselComponentTool tool = new ConfigureProductCarouselComponentTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel\",\"selectionType\":\"product\","
                + "\"product\":[\"MJ01\",\"MJ02#MJ02-black-M\"],\"productCount\":5}"));

        assertTrue(out.get("updated").asBoolean());
        assertTrue(tool.writesContent());
        assertEquals("configure_productcarousel_component", tool.name());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/carousel");
        assertEquals("product", r.getValueMap().get("selectionType", String.class));
        assertArrayEquals(new String[] { "MJ01", "MJ02#MJ02-black-M" }, r.getValueMap().get("product", String[].class));
        assertEquals(Integer.valueOf(5), r.getValueMap().get("productCount", Integer.class));
    }

    @Test
    public void bindsCategorySelection() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel2",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel").commit();

        JsonNode out = new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel2\",\"selectionType\":\"category\","
                + "\"category\":\"Mjk=\",\"productCount\":8}"));

        assertTrue(out.get("updated").asBoolean());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/carousel2");
        assertEquals("category", r.getValueMap().get("selectionType", String.class));
        assertEquals("Mjk=", r.getValueMap().get("category", String.class));
        assertEquals(Integer.valueOf(8), r.getValueMap().get("productCount", Integer.class));
    }

    @Test
    public void emptyProductArrayRemovesExistingProperty() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel3",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel",
            "product", new String[] { "MJ01" }).commit();

        JsonNode out = new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel3\",\"product\":[]}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/carousel3");
        assertNull(r.getValueMap().get("product", String[].class));
    }

    @Test
    public void omittedProductCountRemovesExistingProperty() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel4",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel",
            "productCount", 7).commit();

        JsonNode out = new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel4\",\"selectionType\":\"category\",\"category\":\"Mjk=\"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/carousel4");
        assertNull(r.getValueMap().get("productCount", Integer.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidSelectionType() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel5",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel").commit();

        new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel5\",\"selectionType\":\"bogus\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCarouselResourceType() throws Exception {
        // A non-CIF resource type must fail closed -- MANDATORY negative test for a write tool.
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "core/wcm/components/text/v2/text").commit();

        new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/text\",\"selectionType\":\"product\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/does/not/exist\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingPath() throws Exception {
        new ConfigureProductCarouselComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"selectionType\":\"product\"}"));
    }
}
