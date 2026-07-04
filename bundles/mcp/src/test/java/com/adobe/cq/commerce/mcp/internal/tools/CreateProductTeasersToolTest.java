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

import java.util.Iterator;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateProductTeasersToolTest {

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
    public void createsTeaserPerSkuWithSharedCta() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid");

        CreateProductTeasersTool tool = new CreateProductTeasersTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid\","
                + "\"skus\":[\"MJ01\",\"MJ02\"],\"cta\":\"add-to-cart\"}"));

        assertEquals("/content/site/jcr:content/root/grid", out.get("parentPath").asText());
        assertFalse(out.get("dryRun").asBoolean());
        assertEquals(2, out.get("created").size());

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid");
        int count = 0;
        Iterator<Resource> children = parent.listChildren();
        String firstName = null;
        String secondName = null;
        while (children.hasNext()) {
            Resource child = children.next();
            count++;
            if (firstName == null) {
                firstName = child.getName();
            } else {
                secondName = child.getName();
            }
            assertEquals("core/cif/components/commerce/productteaser/v1/productteaser",
                child.getValueMap().get("sling:resourceType", String.class));
            assertEquals("add-to-cart", child.getValueMap().get("cta", String.class));
            assertEquals("nt:unstructured", child.getValueMap().get("jcr:primaryType", String.class));
            String selection = child.getValueMap().get("selection", String.class);
            assertTrue("MJ01".equals(selection) || "MJ02".equals(selection));
        }
        assertEquals(2, count);
        assertNotEquals(firstName, secondName);

        for (int i = 0; i < out.get("created").size(); i++) {
            JsonNode item = out.get("created").get(i);
            Resource persisted = context.resourceResolver().getResource(item.get("path").asText());
            assertEquals(item.get("selection").asText(), persisted.getValueMap().get("selection", String.class));
        }
    }

    @Test
    public void dryRunReturnsWouldBePathsAndCreatesNothing() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid2");

        CreateProductTeasersTool tool = new CreateProductTeasersTool();
        JsonNode out = tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid2\","
                + "\"skus\":[\"MJ01\",\"MJ02\"],\"dryRun\":true}"));

        assertTrue(out.get("dryRun").asBoolean());
        assertEquals(2, out.get("created").size());
        assertEquals("MJ01", out.get("created").get(0).get("selection").asText());
        assertEquals("MJ02", out.get("created").get(1).get("selection").asText());

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid2");
        assertFalse(parent.listChildren().hasNext());
    }

    @Test
    public void appliesCtaTextToAllCreated() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid3");

        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid3\","
                + "\"skus\":[\"MJ01\"],\"cta\":\"details\",\"ctaText\":\"See details\"}"));

        Resource parent = context.resourceResolver().getResource("/content/site/jcr:content/root/grid3");
        Resource child = parent.listChildren().next();
        assertEquals("details", child.getValueMap().get("cta", String.class));
        assertEquals("See details", child.getValueMap().get("ctaText", String.class));
    }

    @Test
    public void uniqueNamesOnRepeatedCall() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid4");
        CreateProductTeasersTool tool = new CreateProductTeasersTool();

        tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid4\",\"skus\":[\"MJ01\"]}"));
        tool.call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid4\",\"skus\":[\"MJ02\"]}"));

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
    public void rejectsEmptySkus() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid5");

        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid5\",\"skus\":[]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingSkus() throws Exception {
        buildContainer("/content/site/jcr:content/root/grid6");

        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/jcr:content/root/grid6\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingParentPath() throws Exception {
        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"skus\":[\"MJ01\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentPathNotUnderContent() throws Exception {
        context.build().resource("/apps/site/grid", "jcr:primaryType", "nt:unstructured").commit();

        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/apps/site/grid\",\"skus\":[\"MJ01\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonExistentParentPath() throws Exception {
        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/does/not/exist\",\"skus\":[\"MJ01\"]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentPathThatIsACqPage() throws Exception {
        context.create().page("/content/site/apage");

        new CreateProductTeasersTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"/content/site/apage\",\"skus\":[\"MJ01\"]}"));
    }

    @Test
    public void writesContentIsTrue() {
        assertTrue(new CreateProductTeasersTool().writesContent());
    }

    @Test
    public void toolName() {
        assertEquals("create_product_teasers", new CreateProductTeasersTool().name());
    }
}
