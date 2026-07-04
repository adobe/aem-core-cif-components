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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigureCatalogPageToolTest {
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
    public void bindsRootCategory() throws Exception {
        context.build().resource("/content/site/plp/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v3/catalogpage").commit();

        JsonNode out = new ConfigureCatalogPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plp\",\"categoryUid\":\"MjA=\"}"));
        assertTrue(out.get("updated").asBoolean());

        Resource r = context.resourceResolver().getResource("/content/site/plp/jcr:content");
        // The v3/v1 catalog page consumes magentoRootCategoryId (+ type), NOT the productlist "category" property.
        assertEquals("MjA=", r.getValueMap().get("magentoRootCategoryId", String.class));
        assertEquals("uid", r.getValueMap().get("magentoRootCategoryIdType", String.class));
        // showMainCategories defaults to false so the bound root actually scopes the landing navigation.
        assertEquals(Boolean.FALSE, r.getValueMap().get("showMainCategories", Boolean.class));
    }

    @Test
    public void honorsShowMainCategoriesArg() throws Exception {
        context.build().resource("/content/site/plp2/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v1/catalogpage").commit();

        new ConfigureCatalogPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plp2\",\"categoryUid\":\"MjA=\",\"showMainCategories\":true}"));

        Resource r = context.resourceResolver().getResource("/content/site/plp2/jcr:content");
        assertEquals(Boolean.TRUE, r.getValueMap().get("showMainCategories", Boolean.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCifResourceType() throws Exception {
        context.build().resource("/content/site/plainpage/jcr:content",
            "sling:resourceType", "nt:unstructured").commit();

        new ConfigureCatalogPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plainpage\",\"categoryUid\":\"MjA=\"}"));
    }

    @Test
    public void honorsUrlPathIdType() throws Exception {
        context.build().resource("/content/site/plp3/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v3/catalogpage").commit();

        JsonNode out = new ConfigureCatalogPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plp3\",\"categoryUid\":\"MjA=\",\"idType\":\"urlPath\"}"));
        assertTrue(out.get("updated").asBoolean());
        assertEquals("urlPath", out.get("idType").asText());

        Resource r = context.resourceResolver().getResource("/content/site/plp3/jcr:content");
        assertEquals("MjA=", r.getValueMap().get("magentoRootCategoryId", String.class));
        assertEquals("urlPath", r.getValueMap().get("magentoRootCategoryIdType", String.class));
    }

    @Test
    public void defaultsIdTypeToUidWhenOmitted() throws Exception {
        context.build().resource("/content/site/plp4/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v1/catalogpage").commit();

        JsonNode out = new ConfigureCatalogPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plp4\",\"categoryUid\":\"MjA=\"}"));
        assertEquals("uid", out.get("idType").asText());

        Resource r = context.resourceResolver().getResource("/content/site/plp4/jcr:content");
        assertEquals("uid", r.getValueMap().get("magentoRootCategoryIdType", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidIdType() throws Exception {
        context.build().resource("/content/site/plp5/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v1/catalogpage").commit();

        new ConfigureCatalogPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plp5\",\"categoryUid\":\"MjA=\",\"idType\":\"foo\"}"));
    }
}
