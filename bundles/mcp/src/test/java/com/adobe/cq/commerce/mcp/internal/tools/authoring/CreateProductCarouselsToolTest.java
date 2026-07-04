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

import java.util.Iterator;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateProductCarouselsToolTest {

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

    private void buildContainer(String path) {
        context.build().resource(path, "jcr:primaryType", "nt:unstructured").commit();
    }

    @Test
    public void createsCarouselPerSpecWithCorrectProps() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid");

        CreateProductCarouselsTool tool = new CreateProductCarouselsTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid\","
                + "\"carousels\":["
                + "{\"selectionType\":\"product\",\"product\":[\"MJ01\",\"MJ02\"],\"productCount\":5},"
                + "{\"selectionType\":\"category\",\"category\":\"MjA=\"}"
                + "]}"));

        assertEquals("/content/site/jcr:content/root/grid", out.get("parentPath").asText());
        assertFalse(out.get("dryRun").asBoolean());
        assertEquals(2, out.get("created").size());

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid");
        int count = 0;
        Iterator<Resource> children = parent.listChildren();
        String firstName = null;
        String secondName = null;
        Resource productCarousel = null;
        Resource categoryCarousel = null;
        while (children.hasNext()) {
            Resource child = children.next();
            count++;
            if (firstName == null) {
                firstName = child.getName();
            } else {
                secondName = child.getName();
            }
            assertEquals("core/cif/components/commerce/productcarousel/v1/productcarousel",
                child.getValueMap().get("sling:resourceType", String.class));
            assertEquals("nt:unstructured", child.getValueMap().get("jcr:primaryType", String.class));
            String selectionType = child.getValueMap().get("selectionType", String.class);
            if ("product".equals(selectionType)) {
                productCarousel = child;
            } else if ("category".equals(selectionType)) {
                categoryCarousel = child;
            }
        }
        assertEquals(2, count);
        assertNotEquals(firstName, secondName);

        assertArrayEquals(new String[] { "MJ01", "MJ02" }, productCarousel.getValueMap().get("product", String[].class));
        assertEquals(Integer.valueOf(5), productCarousel.getValueMap().get("productCount", Integer.class));
        assertEquals("product", productCarousel.getValueMap().get("selectionType", String.class));

        assertEquals("MjA=", categoryCarousel.getValueMap().get("category", String.class));
        assertEquals("category", categoryCarousel.getValueMap().get("selectionType", String.class));
        assertNull(categoryCarousel.getValueMap().get("product", String[].class));
        assertNull(categoryCarousel.getValueMap().get("productCount", Integer.class));

        for (int i = 0; i < out.get("created").size(); i++) {
            JsonNode item = out.get("created").get(i);
            Resource persisted = context.resourceResolver().getResource(item.get("path").asText());
            assertEquals("core/cif/components/commerce/productcarousel/v1/productcarousel",
                persisted.getValueMap().get("sling:resourceType", String.class));
        }
    }

    @Test
    public void combinedSkuNormalizesProductEntries() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid1b");

        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid1b\","
                + "\"carousels\":[{\"product\":[\"MJ01\"]}]}"));

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid1b");
        Resource child = parent.listChildren().next();
        assertArrayEquals(new String[] { "MJ01" }, child.getValueMap().get("product", String[].class));
    }

    @Test
    public void dryRunReturnsWouldBePathsAndCreatesNothing() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid2");

        CreateProductCarouselsTool tool = new CreateProductCarouselsTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid2\","
                + "\"carousels\":["
                + "{\"selectionType\":\"product\",\"product\":[\"MJ01\"]},"
                + "{\"selectionType\":\"category\",\"category\":\"MjA=\"}"
                + "],\"dryRun\":true}"));

        assertTrue(out.get("dryRun").asBoolean());
        assertEquals(2, out.get("created").size());
        assertNotEquals(out.get("created").get(0).get("path").asText(), out.get("created").get(1).get("path").asText());

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid2");
        assertFalse(parent.listChildren().hasNext());
    }

    @Test
    public void uniqueNamesOnRepeatedCall() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid4");
        CreateProductCarouselsTool tool = new CreateProductCarouselsTool();

        tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid4\","
                + "\"carousels\":[{\"selectionType\":\"product\",\"product\":[\"MJ01\"]}]}"));
        tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid4\","
                + "\"carousels\":[{\"selectionType\":\"product\",\"product\":[\"MJ02\"]}]}"));

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid4");
        int count = 0;
        Iterator<Resource> it = parent.listChildren();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidSelectionType() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid5");

        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid5\","
                + "\"carousels\":[{\"selectionType\":\"bogus\"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyCarousels() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid6");

        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid6\",\"carousels\":[]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingCarousels() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid7");

        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid7\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingParentPath() throws Exception {
        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"carousels\":[{\"selectionType\":\"product\",\"product\":[\"MJ01\"]}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentPathNotUnderContent() throws Exception {
        context.build().resource("/apps/site/grid", "jcr:primaryType", "nt:unstructured").commit();

        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/apps/site/grid\",\"carousels\":[{\"selectionType\":\"product\",\"product\":[\"MJ01\"]}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonExistentParentPath() throws Exception {
        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/does/not/exist\","
                + "\"carousels\":[{\"selectionType\":\"product\",\"product\":[\"MJ01\"]}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentPathThatIsACqPage() throws Exception {
        context.create().page("/content/site/apage");

        new CreateProductCarouselsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/apage\","
                + "\"carousels\":[{\"selectionType\":\"product\",\"product\":[\"MJ01\"]}]}"));
    }

    @Test
    public void writesContentIsTrue() {
        assertTrue(new CreateProductCarouselsTool().writesContent());
    }

    @Test
    public void toolName() {
        assertEquals("create_product_carousels", new CreateProductCarouselsTool().name());
    }
}
