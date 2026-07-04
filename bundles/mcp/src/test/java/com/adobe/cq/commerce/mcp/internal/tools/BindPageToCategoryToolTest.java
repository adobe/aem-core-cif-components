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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BindPageToCategoryToolTest {
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
    public void writesSelectorFilterAsUidAndUrlPath() throws Exception {
        createStructurePage("/content/site/category-page");
        context.resourceResolver().commit();

        JsonNode out = new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/category-page\",\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\","
                + "\"includesSubCategories\":true}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals("/content/site/category-page", out.get("path").asText());
        assertEquals("MjA=|venia-tops", out.get("selectorFilter").get(0).asText());
        assertTrue(out.get("includesSubCategories").asBoolean());

        Resource content = context.resourceResolver().getResource("/content/site/category-page/jcr:content");
        String[] selectorFilter = content.getValueMap().get("selectorFilter", String[].class);
        assertArrayEquals(new String[] { "MjA=|venia-tops" }, selectorFilter);
        assertEquals("uidAndUrlPath", content.getValueMap().get("selectorFilterType", String.class));
        assertEquals(Boolean.TRUE, content.getValueMap().get("includesSubCategories", Boolean.class));
    }

    @Test
    public void includesSubCategoriesDefaultsToFalseWhenOmitted() throws Exception {
        createStructurePage("/content/site/category-page-default");
        context.resourceResolver().commit();

        JsonNode out = new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/category-page-default\",\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}"));

        assertFalse(out.get("includesSubCategories").asBoolean());
        Resource content = context.resourceResolver().getResource("/content/site/category-page-default/jcr:content");
        assertEquals(Boolean.FALSE, content.getValueMap().get("includesSubCategories", Boolean.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingPath() throws Exception {
        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingCategoryUid() throws Exception {
        createStructurePage("/content/site/category-page4");
        context.resourceResolver().commit();

        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/category-page4\",\"urlPath\":\"venia-tops\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingUrlPath() throws Exception {
        createStructurePage("/content/site/category-page5");
        context.resourceResolver().commit();

        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/category-page5\",\"categoryUid\":\"MjA=\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankCategoryUid() throws Exception {
        createStructurePage("/content/site/category-page6");
        context.resourceResolver().commit();

        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/category-page6\",\"categoryUid\":\"   \",\"urlPath\":\"venia-tops\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankUrlPath() throws Exception {
        createStructurePage("/content/site/category-page7");
        context.resourceResolver().commit();

        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/category-page7\",\"categoryUid\":\"MjA=\",\"urlPath\":\"   \"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCifPage() throws Exception {
        // A page whose jcr:content is not a CIF structure page type must fail closed.
        context.create().page("/content/site/plainpage");
        context.resourceResolver().commit();

        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plainpage\",\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPageResource() throws Exception {
        // A component resource (not a cq:Page at all) must fail closed -- it does not adapt to Page.
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        new BindPageToCategoryTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser\",\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}"));
    }

    @Test
    public void writesContentIsTrue() {
        assertTrue(new BindPageToCategoryTool().writesContent());
    }

    @Test
    public void toolNameIsExpected() {
        assertEquals("bind_page_to_category", new BindPageToCategoryTool().name());
        assertFalse(new BindPageToCategoryTool().description().isEmpty());
    }
}
