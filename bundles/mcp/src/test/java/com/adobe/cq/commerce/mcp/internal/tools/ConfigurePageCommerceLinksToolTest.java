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
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurePageCommerceLinksToolTest {
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
    public void writesNavConfigPagefields() throws Exception {
        createStructurePage("/content/site/navroot");
        context.resourceResolver().commit();

        JsonNode out = new ConfigurePageCommerceLinksTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/navroot\","
                + "\"cifProductPage\":\"/content/venia/us/en/products\","
                + "\"cifCategoryPage\":\"/content/venia/us/en/category\","
                + "\"cifSearchResultsPage\":\"/content/venia/us/en/search\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals("/content/site/navroot", out.get("path").asText());

        Resource content = context.resourceResolver().getResource("/content/site/navroot/jcr:content");
        assertEquals("/content/venia/us/en/products", content.getValueMap().get("cq:cifProductPage", String.class));
        assertEquals("/content/venia/us/en/category", content.getValueMap().get("cq:cifCategoryPage", String.class));
        assertEquals("/content/venia/us/en/search", content.getValueMap().get("cq:cifSearchResultsPage", String.class));
    }

    @Test
    public void writesOnlyOneProvidedField() throws Exception {
        createStructurePage("/content/site/navroot2");
        context.resourceResolver().commit();

        new ConfigurePageCommerceLinksTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/navroot2\",\"cifProductPage\":\"/content/venia/us/en/products\"}"));

        Resource content = context.resourceResolver().getResource("/content/site/navroot2/jcr:content");
        assertEquals("/content/venia/us/en/products", content.getValueMap().get("cq:cifProductPage", String.class));
        assertNull(content.getValueMap().get("cq:cifCategoryPage", String.class));
        assertNull(content.getValueMap().get("cq:cifSearchResultsPage", String.class));
    }

    @Test
    public void blankClearsExistingValue() throws Exception {
        createStructurePage("/content/site/navroot3");
        context.resourceResolver().getResource("/content/site/navroot3/jcr:content")
            .adaptTo(ModifiableValueMap.class)
            .put("cq:cifProductPage", "/content/venia/us/en/products-old");
        context.resourceResolver().commit();

        JsonNode out = new ConfigurePageCommerceLinksTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/navroot3\",\"cifProductPage\":\"\","
                + "\"cifCategoryPage\":\"/content/venia/us/en/category\"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource content = context.resourceResolver().getResource("/content/site/navroot3/jcr:content");
        assertNull(content.getValueMap().get("cq:cifProductPage", String.class));
        assertEquals("/content/venia/us/en/category", content.getValueMap().get("cq:cifCategoryPage", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWhenNoOptionalsProvided() throws Exception {
        createStructurePage("/content/site/navroot4");
        context.resourceResolver().commit();

        new ConfigurePageCommerceLinksTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/navroot4\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCifPage() throws Exception {
        // A page whose jcr:content is not a CIF structure page type must fail closed.
        context.create().page("/content/site/plainpage");
        context.resourceResolver().commit();

        new ConfigurePageCommerceLinksTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plainpage\",\"cifProductPage\":\"/content/venia/us/en/products\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPageResource() throws Exception {
        // A component resource (not a cq:Page at all) must fail closed -- it does not adapt to Page.
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        new ConfigurePageCommerceLinksTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser\",\"cifProductPage\":\"/content/venia/us/en/products\"}"));
    }

    @Test
    public void writesContentIsTrue() {
        assertTrue(new ConfigurePageCommerceLinksTool().writesContent());
    }

    @Test
    public void toolNameIsExpected() {
        assertEquals("configure_page_commerce_links", new ConfigurePageCommerceLinksTool().name());
        assertFalse(new ConfigurePageCommerceLinksTool().description().isEmpty());
    }
}
