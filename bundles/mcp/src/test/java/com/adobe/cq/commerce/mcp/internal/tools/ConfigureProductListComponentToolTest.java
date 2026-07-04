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

public class ConfigureProductListComponentToolTest {
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
    public void pinsCategoryOnProductListComponent() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productlist",
            "sling:resourceType", "core/cif/components/commerce/productlist/v2/productlist").commit();

        ConfigureProductListComponentTool tool = new ConfigureProductListComponentTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productlist\",\"categoryUid\":\"MjA=\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertTrue(tool.writesContent());
        assertEquals("configure_productlist_component", tool.name());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/productlist");
        assertEquals("MjA=", r.getValueMap().get("category", String.class));
    }

    @Test
    public void acceptsProductCarouselComponent() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel").commit();

        JsonNode out = new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel\",\"categoryUid\":\"NQ==\"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/carousel");
        assertEquals("NQ==", r.getValueMap().get("category", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonProductListComponent() throws Exception {
        // A catalog page structure component is NOT a product-list component — must fail closed.
        context.build().resource("/content/site/plp/jcr:content",
            "sling:resourceType", "core/cif/components/structure/catalogpage/v3/catalogpage").commit();

        new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plp/jcr:content\",\"categoryUid\":\"MjA=\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() throws Exception {
        new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/does/not/exist\",\"categoryUid\":\"MjA=\"}"));
    }

    @Test
    public void writesFullDialogPropertiesWithCorrectTypes() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productlist2",
            "sling:resourceType", "core/cif/components/commerce/productlist/v2/productlist").commit();

        JsonNode out = new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productlist2\","
                + "\"showTitle\":true,\"showImage\":false,\"pageSize\":12,"
                + "\"defaultSortField\":\"price\",\"defaultSortOrder\":\"desc\"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/productlist2");
        assertEquals(Boolean.TRUE, r.getValueMap().get("showTitle", Boolean.class));
        assertEquals(Boolean.FALSE, r.getValueMap().get("showImage", Boolean.class));
        assertEquals(Integer.valueOf(12), r.getValueMap().get("pageSize", Integer.class));
        assertEquals("price", r.getValueMap().get("defaultSortField", String.class));
        assertEquals("desc", r.getValueMap().get("defaultSortOrder", String.class));
    }

    @Test
    public void writesFragmentsCompositeMultifield() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productlist3",
            "sling:resourceType", "core/cif/components/commerce/productlist/v2/productlist").commit();

        JsonNode out = new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productlist3\","
                + "\"fragments\":[{\"fragmentLocation\":\"loc\",\"fragmentPage\":1,\"fragmentCssClass\":\"c\"}]}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/productlist3");
        Resource item0 = r.getChild("fragments").getChild("item0");
        assertEquals("loc", item0.getValueMap().get("fragmentLocation", String.class));
        assertEquals(Integer.valueOf(1), item0.getValueMap().get("fragmentPage", Integer.class));
        assertEquals("c", item0.getValueMap().get("fragmentCssClass", String.class));
    }

    @Test
    public void categoryOnlyCallStillWorksUnchanged() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productlist4",
            "sling:resourceType", "core/cif/components/commerce/productlist/v1/productlist").commit();

        JsonNode out = new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productlist4\",\"categoryUid\":\"MjA=\"}"));

        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/productlist4");
        assertEquals("MjA=", r.getValueMap().get("category", String.class));
        assertNull(r.getValueMap().get("showTitle", Boolean.class));
        assertNull(r.getValueMap().get("pageSize", Integer.class));
        assertNull(r.getChild("fragments"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidDefaultSortOrder() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productlist5",
            "sling:resourceType", "core/cif/components/commerce/productlist/v2/productlist").commit();

        new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productlist5\",\"defaultSortOrder\":\"bogus\"}"));
    }

    @Test
    public void fractionalPageSizeIsNotWritten() throws Exception {
        context.build().resource("/content/site/jcr:content/root/productlist6",
            "sling:resourceType", "core/cif/components/commerce/productlist/v2/productlist").commit();

        new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/productlist6\",\"pageSize\":1.9}"));

        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/productlist6");
        assertNull(r.getValueMap().get("pageSize", Integer.class));
    }

    @Test
    public void newPropsAreNoOpOnCarouselButStillUpdateCategory() throws Exception {
        context.build().resource("/content/site/jcr:content/root/carousel2",
            "sling:resourceType", "core/cif/components/commerce/productcarousel/v1/productcarousel").commit();

        JsonNode out = new ConfigureProductListComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/carousel2\",\"categoryUid\":\"NQ==\","
                + "\"showTitle\":true,\"pageSize\":12}"));

        // The write itself is not gated per-property, so showTitle/pageSize are still written to the node -- but
        // ProductCarouselImpl never reads them (it doesn't extend ProductCollectionImpl/ProductListImpl), so they
        // are inert no-ops at render time. categoryUid is the property the carousel actually consumes.
        assertTrue(out.get("updated").asBoolean());
        Resource r = context.resourceResolver().getResource("/content/site/jcr:content/root/carousel2");
        assertEquals("NQ==", r.getValueMap().get("category", String.class));
    }
}
