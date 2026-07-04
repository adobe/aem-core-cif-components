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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigureProductVisibleSectionsToolTest {
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
    public void writesValidatedVisibleSections() throws Exception {
        context.build().resource("/content/site/jcr:content/root/product",
            "sling:resourceType", "core/cif/components/commerce/product/v3/product").commit();

        ConfigureProductVisibleSectionsTool tool = new ConfigureProductVisibleSectionsTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/product\",\"visibleSections\":[\"title\",\"price\",\"images\"]}"));

        assertTrue(out.get("updated").asBoolean());
        assertTrue(tool.writesContent());
        assertTrue(tool.name().equals("configure_product_visible_sections"));

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/product");
        // Real write -> readback, not mock-echo.
        assertArrayEquals(new String[] { "title", "price", "images" },
            r.getValueMap().get("visibleSections", String[].class));
    }

    @Test
    public void emptyArrayClearsExistingValue() throws Exception {
        context.build().resource("/content/site/jcr:content/root/product2",
            "sling:resourceType", "core/cif/components/commerce/product/v3/product",
            "visibleSections", new String[] { "title", "price" }).commit();

        JsonNode out = new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/product2\",\"visibleSections\":[]}"));

        assertTrue(out.get("updated").asBoolean());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/product2");
        assertNull(r.getValueMap().get("visibleSections", String[].class));
    }

    @Test
    public void acceptsProductV1AndV2Too() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productv1",
            "sling:resourceType", "core/cif/components/commerce/product/v1/product").commit();
        context.build().resource("/content/site/jcr:content/root/productv2",
            "sling:resourceType", "core/cif/components/commerce/product/v2/product").commit();

        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productv1\",\"visibleSections\":[\"sku\"]}"));
        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productv2\",\"visibleSections\":[\"sku\"]}"));

        assertArrayEquals(new String[] { "sku" }, context.resourceResolver()
            .getResource("/content/site/jcr:content/root/productv1").getValueMap().get("visibleSections", String[].class));
        assertArrayEquals(new String[] { "sku" }, context.resourceResolver()
            .getResource("/content/site/jcr:content/root/productv2").getValueMap().get("visibleSections", String[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidToken() throws Exception {
        context.build().resource("/content/site/jcr:content/root/product3",
            "sling:resourceType", "core/cif/components/commerce/product/v3/product").commit();

        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/product3\",\"visibleSections\":[\"image\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownToken() throws Exception {
        context.build().resource("/content/site/jcr:content/root/product4",
            "sling:resourceType", "core/cif/components/commerce/product/v3/product").commit();

        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/product4\",\"visibleSections\":[\"foo\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingVisibleSectionsArg() throws Exception {
        context.build().resource("/content/site/jcr:content/root/product5",
            "sling:resourceType", "core/cif/components/commerce/product/v3/product").commit();

        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/product5\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonProductResourceType() throws Exception {
        // A non-CIF resource type must fail closed -- MANDATORY negative test for a write tool.
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "core/wcm/components/text/v2/text").commit();

        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/text\",\"visibleSections\":[\"title\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        new ConfigureProductVisibleSectionsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/does/not/exist\",\"visibleSections\":[\"title\"]}"));
    }
}
