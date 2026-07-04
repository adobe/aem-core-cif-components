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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigureFeaturedCategoryListComponentToolTest {
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
    public void writesItemsAndTitleForFeaturedCategoryList() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();

        ConfigureFeaturedCategoryListComponentTool tool = new ConfigureFeaturedCategoryListComponentTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl\","
                + "\"items\":[{\"categoryId\":\"MjA=\"},{\"categoryId\":\"Mjk=\",\"asset\":\"/content/dam/x.jpg\"}],"
                + "\"title\":\"Shop\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals(2, out.get("itemCount").asInt());
        assertTrue(tool.writesContent());
        assertEquals("configure_featuredcategorylist_component", tool.name());

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/fcl");
        assertEquals("Shop", r.getValueMap().get("jcr:title", String.class));
        Resource items = r.getChild("items");
        assertEquals("MjA=", items.getChild("item0").getValueMap().get("categoryId", String.class));
        assertEquals("Mjk=", items.getChild("item1").getValueMap().get("categoryId", String.class));
        assertEquals("/content/dam/x.jpg", items.getChild("item1").getValueMap().get("asset", String.class));
    }

    @Test
    public void acceptsCategoryCarouselResourceType() throws Exception {
        context.build().resource("/content/site/jcr:content/root/cc",
            "sling:resourceType", "core/cif/components/commerce/categorycarousel/v1/categorycarousel").commit();

        JsonNode out = new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/cc\",\"items\":[{\"categoryId\":\"MjA=\"}]}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals(1, out.get("itemCount").asInt());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/cc");
        assertEquals("MjA=", r.getChild("items").getChild("item0").getValueMap().get("categoryId", String.class));
    }

    @Test
    public void rewriteWithFewerItemsReplacesPriorChildren() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl2",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();
        ConfigureFeaturedCategoryListComponentTool tool = new ConfigureFeaturedCategoryListComponentTool();
        tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl2\","
                + "\"items\":[{\"categoryId\":\"AAA=\"},{\"categoryId\":\"BBB=\"},{\"categoryId\":\"CCC=\"}]}"));

        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl2\",\"items\":[{\"categoryId\":\"DDD=\"}]}"));

        assertTrue(out.get("updated").asBoolean());
        assertEquals(1, out.get("itemCount").asInt());
        Resource items = context.resourceResolver().getResource("/content/site/jcr:content/root/fcl2/items");
        assertEquals("DDD=", items.getChild("item0").getValueMap().get("categoryId", String.class));
        assertNull(items.getChild("item1"));
        assertNull(items.getChild("item2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsItemMissingCategoryId() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl3",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl3\",\"items\":[{\"asset\":\"/content/dam/x.jpg\"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsItemWithBlankCategoryId() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl4",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl4\",\"items\":[{\"categoryId\":\"   \"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingItems() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl5",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl5\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyItems() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl6",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl6\",\"items\":[]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCifResourceType() throws Exception {
        // MANDATORY negative test for a write tool: a non-CIF resource type must fail closed.
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "core/wcm/components/text/v2/text").commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/text\",\"items\":[{\"categoryId\":\"MjA=\"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/does/not/exist\",\"items\":[{\"categoryId\":\"MjA=\"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingPath() throws Exception {
        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"items\":[{\"categoryId\":\"MjA=\"}]}"));
    }

    @Test
    public void writesOptionalTitleTypeAndLinkTarget() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl7",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist")
            .commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl7\",\"items\":[{\"categoryId\":\"MjA=\"}],"
                + "\"titleType\":\"h2\",\"linkTarget\":\"_blank\"}"));

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/fcl7");
        assertEquals("h2", r.getValueMap().get("titleType", String.class));
        assertEquals("_blank", r.getValueMap().get("linkTarget", String.class));
    }

    @Test
    public void blankTitleRemovesExistingProperty() throws Exception {
        context.build().resource("/content/site/jcr:content/root/fcl8",
            "sling:resourceType", "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist",
            "jcr:title", "Old Title").commit();

        new ConfigureFeaturedCategoryListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/fcl8\",\"items\":[{\"categoryId\":\"MjA=\"}],\"title\":\"\"}"));

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/fcl8");
        assertNull(r.getValueMap().get("jcr:title", String.class));
    }
}
