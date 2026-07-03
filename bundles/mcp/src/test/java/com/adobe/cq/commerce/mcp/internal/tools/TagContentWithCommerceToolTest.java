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

public class TagContentWithCommerceToolTest {
    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private void createDamAsset(String path) {
        context.create().resource(path, "jcr:primaryType", "dam:Asset");
        context.create().resource(path + "/jcr:content", "jcr:primaryType", "nt:unstructured");
        context.create().resource(path + "/jcr:content/metadata", "jcr:primaryType", "nt:unstructured");
    }

    @Test
    public void tagsDamAssetWithProductSku() throws Exception {
        createDamAsset("/content/dam/venia/sample.jpg");

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        TagContentWithCommerceTool tool = new TagContentWithCommerceTool();
        JsonNode out = tool.call(ctx, mapper.readTree(
            "{\"path\":\"/content/dam/venia/sample.jpg\",\"sku\":\"VP11\",\"action\":\"add\"}"));

        assertTrue(out.get("updated").asBoolean());
        assertTrue(tool.writesContent());
        assertEquals("VP11", out.get("products").get(0).asText());
    }

    @Test
    public void tagsPageWithCategoryUid() throws Exception {
        context.create().resource("/content/venia/us/en/sample",
            "jcr:primaryType", "cq:Page");
        context.create().resource("/content/venia/us/en/sample/jcr:content",
            "jcr:primaryType", "cq:PageContent");

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        TagContentWithCommerceTool tool = new TagContentWithCommerceTool();
        JsonNode out = tool.call(ctx, mapper.readTree(
            "{\"path\":\"/content/venia/us/en/sample\",\"categoryUid\":\"MjA=\",\"action\":\"add\"}"));

        assertEquals("MjA=", out.get("categories").get(0).asText());
    }

    @Test
    public void appendsWithoutDuplicatingExistingTags() throws Exception {
        createDamAsset("/content/dam/venia/sample.jpg");
        ModifiableValueMap metadata = context.resourceResolver()
            .getResource("/content/dam/venia/sample.jpg/jcr:content/metadata")
            .adaptTo(ModifiableValueMap.class);
        metadata.put("cq:products", new String[] { "VD09" });

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        new TagContentWithCommerceTool().call(ctx, mapper.readTree(
            "{\"path\":\"/content/dam/venia/sample.jpg\",\"sku\":\"VP11\",\"action\":\"add\"}"));

        String[] products = context.resourceResolver()
            .getResource("/content/dam/venia/sample.jpg/jcr:content/metadata")
            .getValueMap()
            .get("cq:products", String[].class);
        assertEquals(2, products.length);
        assertTrue(java.util.Arrays.asList(products).contains("VD09"));
        assertTrue(java.util.Arrays.asList(products).contains("VP11"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedResource() throws Exception {
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "foundation/components/text").commit();

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        new TagContentWithCommerceTool().call(ctx, mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/text\",\"sku\":\"VP11\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGenericNodeThatMerelyHasJcrContentChild() throws Exception {
        // Not a DAM asset, page, or experience-fragment variation — just an arbitrary node that happens to have a
        // jcr:content child. Must fail closed on resource type rather than tag it.
        context.create().resource("/content/generic-node", "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/generic-node/jcr:content", "jcr:primaryType", "nt:unstructured");

        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);

        new TagContentWithCommerceTool().call(ctx, mapper.readTree(
            "{\"path\":\"/content/generic-node\",\"sku\":\"VP11\"}"));
    }
}
