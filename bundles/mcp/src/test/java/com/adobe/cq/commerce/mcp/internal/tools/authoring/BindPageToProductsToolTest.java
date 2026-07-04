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
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BindPageToProductsToolTest {
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

    private void createStructurePage(String path) {
        context.create().page(path);
        context.resourceResolver().getResource(path + "/jcr:content")
            .adaptTo(ModifiableValueMap.class)
            .put("sling:resourceType", "core/cif/components/structure/page/v3/page");
    }

    @Test
    public void writesSelectorFilterAsPlainSlugs() throws Exception {
        createStructurePage("/content/site/product-page");
        context.resourceResolver().commit();

        JsonNode out = new BindPageToProductsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/product-page\",\"skusOrUrlKeys\":[\"jillian-top\",\"vitalia-top\"]}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals("/content/site/product-page", out.get("path").asText());
        assertEquals("jillian-top", out.get("selectorFilter").get(0).asText());
        assertEquals("vitalia-top", out.get("selectorFilter").get(1).asText());

        Resource content = context.resourceResolver().getResource("/content/site/product-page/jcr:content");
        String[] selectorFilter = content.getValueMap().get("selectorFilter", String[].class);
        assertArrayEquals(new String[] { "jillian-top", "vitalia-top" }, selectorFilter);
    }

    @Test
    public void emptyArrayClearsSelectorFilter() throws Exception {
        createStructurePage("/content/site/product-page2");
        context.resourceResolver().getResource("/content/site/product-page2/jcr:content")
            .adaptTo(ModifiableValueMap.class)
            .put("selectorFilter", new String[] { "old-slug" });
        context.resourceResolver().commit();

        JsonNode out = new BindPageToProductsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/product-page2\",\"skusOrUrlKeys\":[]}"));

        assertTrue(out.get("updated").asBoolean());
        Resource content = context.resourceResolver().getResource("/content/site/product-page2/jcr:content");
        assertNull(content.getValueMap().get("selectorFilter", String[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingPath() throws Exception {
        new BindPageToProductsTool().call(ctxForResolver(), mapper.readTree(
            "{\"skusOrUrlKeys\":[\"jillian-top\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingSkusOrUrlKeys() throws Exception {
        createStructurePage("/content/site/product-page3");
        context.resourceResolver().commit();

        new BindPageToProductsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/product-page3\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCifPage() throws Exception {
        // A page whose jcr:content is not a CIF structure page type must fail closed.
        context.create().page("/content/site/plainpage");
        context.resourceResolver().commit();

        new BindPageToProductsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plainpage\",\"skusOrUrlKeys\":[\"jillian-top\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPageResource() throws Exception {
        // A component resource (not a cq:Page at all) must fail closed -- it does not adapt to Page.
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        new BindPageToProductsTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser\",\"skusOrUrlKeys\":[\"jillian-top\"]}"));
    }

    @Test
    public void writesContentIsTrue() {
        assertTrue(new BindPageToProductsTool().writesContent());
    }

    @Test
    public void toolNameIsExpected() {
        assertEquals("bind_page_to_products", new BindPageToProductsTool().name());
        assertFalse(new BindPageToProductsTool().description().isEmpty());
    }
}
