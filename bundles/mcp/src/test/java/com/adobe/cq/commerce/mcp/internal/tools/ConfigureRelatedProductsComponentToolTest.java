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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigureRelatedProductsComponentToolTest {
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
    public void bindsPlainSkuAndRelationType() throws Exception {
        context.build().resource("/content/site/jcr:content/root/relatedproducts",
            "sling:resourceType", "core/cif/components/commerce/relatedproducts/v1/relatedproducts").commit();

        ConfigureRelatedProductsComponentTool tool = new ConfigureRelatedProductsComponentTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/relatedproducts\",\"product\":\"MJ01\",\"relationType\":\"UPSELL_PRODUCTS\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals("UPSELL_PRODUCTS", out.get("relationType").asText());
        assertTrue(tool.writesContent());
        assertEquals("configure_relatedproducts_component", tool.name());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/relatedproducts");
        // product must be the plain SKU, NOT combinedSku-encoded.
        assertEquals("MJ01", r.getValueMap().get("product", String.class));
        assertEquals("UPSELL_PRODUCTS", r.getValueMap().get("relationType", String.class));
    }

    @Test
    public void blankProductRemovesExistingValue() throws Exception {
        context.build().resource("/content/site/jcr:content/root/relatedproducts2",
            "sling:resourceType", "core/cif/components/commerce/relatedproducts/v1/relatedproducts",
            "product", "MJ01",
            "relationType", "RELATED_PRODUCTS").commit();

        JsonNode out = new ConfigureRelatedProductsComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/relatedproducts2\",\"product\":\"  \",\"relationType\":\"CROSS_SELL_PRODUCTS\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals("CROSS_SELL_PRODUCTS", out.get("relationType").asText());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/relatedproducts2");
        assertNull(r.getValueMap().get("product", String.class));
        assertEquals("CROSS_SELL_PRODUCTS", r.getValueMap().get("relationType", String.class));
    }

    @Test
    public void omittedProductDoesNotFailAndRemovesExistingValue() throws Exception {
        context.build().resource("/content/site/jcr:content/root/relatedproducts3",
            "sling:resourceType", "core/cif/components/commerce/relatedproducts/v1/relatedproducts",
            "product", "MJ01",
            "relationType", "RELATED_PRODUCTS").commit();

        JsonNode out = new ConfigureRelatedProductsComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/relatedproducts3\",\"relationType\":\"UPSELL_PRODUCTS\"}"));

        assertTrue(out.get("updated").asBoolean());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/relatedproducts3");
        assertNull(r.getValueMap().get("product", String.class));
        assertEquals("UPSELL_PRODUCTS", r.getValueMap().get("relationType", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidRelationType() throws Exception {
        context.build().resource("/content/site/jcr:content/root/relatedproducts4",
            "sling:resourceType", "core/cif/components/commerce/relatedproducts/v1/relatedproducts").commit();

        new ConfigureRelatedProductsComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/relatedproducts4\",\"relationType\":\"NOT_A_REAL_TYPE\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingRelationType() throws Exception {
        context.build().resource("/content/site/jcr:content/root/relatedproducts5",
            "sling:resourceType", "core/cif/components/commerce/relatedproducts/v1/relatedproducts").commit();

        new ConfigureRelatedProductsComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/relatedproducts5\",\"product\":\"MJ01\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonRelatedProductsResourceType() throws Exception {
        // A non-CIF resource type must fail closed -- MANDATORY negative test for a write tool.
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "core/wcm/components/text/v2/text").commit();

        new ConfigureRelatedProductsComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/text\",\"relationType\":\"RELATED_PRODUCTS\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        new ConfigureRelatedProductsComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/does/not/exist\",\"relationType\":\"RELATED_PRODUCTS\"}"));
    }
}
