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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigureProductTeaserComponentToolTest {
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
    public void bindsSkuAndOptionalPropsOnTeaser() throws Exception {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        ConfigureProductTeaserComponentTool tool = new ConfigureProductTeaserComponentTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser\",\"sku\":\"MJ01\",\"cta\":\"add-to-cart\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals("MJ01", out.get("selection").asText());
        assertTrue(tool.writesContent());
        assertEquals("configure_productteaser_component", tool.name());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/teaser");
        assertEquals("MJ01", r.getValueMap().get("selection", String.class));
        assertEquals("add-to-cart", r.getValueMap().get("cta", String.class));
    }

    @Test
    public void normalizesCombinedSkuAndWritesAllOptionalProps() throws Exception {
        context.build().resource("/content/site/jcr:content/root/teaser2",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        JsonNode out = new ConfigureProductTeaserComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser2\",\"sku\":\"MJ01#MJ01-black-M\","
                + "\"cta\":\"details\",\"ctaText\":\"View details\",\"linkTarget\":\"_blank\",\"id\":\"my-teaser\"}"));

        assertEquals("MJ01#MJ01-black-M", out.get("selection").asText());
        assertTrue(out.get("updated").asBoolean());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/teaser2");
        assertEquals("MJ01#MJ01-black-M", r.getValueMap().get("selection", String.class));
        assertEquals("details", r.getValueMap().get("cta", String.class));
        assertEquals("View details", r.getValueMap().get("ctaText", String.class));
        assertEquals("_blank", r.getValueMap().get("linkTarget", String.class));
        assertEquals("my-teaser", r.getValueMap().get("id", String.class));
        // teaser model reads selection only -- selectionType must NOT be written.
        assertEquals(null, r.getValueMap().get("selectionType", String.class));
    }

    @Test
    public void whitespaceOnlyOptionalClearsExistingValue() throws Exception {
        // A whitespace-only optional value is never a meaningful commerce value -- it must clear the property
        // (isBlank semantics), not persist " " (previously the case with the old isNotEmpty-based local helper).
        context.build().resource("/content/site/jcr:content/root/teaser4",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser",
            "cta", "add-to-cart").commit();

        JsonNode out = new ConfigureProductTeaserComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser4\",\"sku\":\"MJ01\",\"cta\":\"  \"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/teaser4");
        assertEquals(null, r.getValueMap().get("cta", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonTeaserResourceType() throws Exception {
        // A non-CIF resource type must fail closed -- MANDATORY negative test for a write tool.
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "core/wcm/components/text/v2/text").commit();

        new ConfigureProductTeaserComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/text\",\"sku\":\"MJ01\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingSku() throws Exception {
        context.build().resource("/content/site/jcr:content/root/teaser3",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        new ConfigureProductTeaserComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser3\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        new ConfigureProductTeaserComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/does/not/exist\",\"sku\":\"MJ01\"}"));
    }
}
