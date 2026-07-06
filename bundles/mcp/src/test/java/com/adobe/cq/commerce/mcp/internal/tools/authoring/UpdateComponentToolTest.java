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
import org.apache.sling.api.resource.ValueMap;
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

public class UpdateComponentToolTest {

    private static final String COMPONENT = "/content/mysite/en/jcr:content/root/teaser";

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

    private void buildFixture() {
        context.build()
            .resource("/apps/mysite/components/banner",
                "jcr:primaryType", "cq:Component",
                "jcr:title", "Banner")
            .resource("/content/mysite/en", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent",
                "sling:resourceType", "mysite/components/page")
            .resource("root", "sling:resourceType", "wcm/foundation/components/responsivegrid")
            .resource("teaser", "sling:resourceType", "mysite/components/teaser",
                "jcr:title", "Old", "obsolete", "remove-me")
            .commit();
    }

    @Test
    public void setsAndRemovesPropertiesAndVerifiesPersistence() throws Exception {
        buildFixture();

        JsonNode out = new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"properties\":{"
                + "\"jcr:title\":\"New\",\"count\":7,\"active\":true,\"tags\":[\"x\",\"y\"],"
                + "\"obsolete\":null}}"));

        assertFalse(out.get("dryRun").asBoolean());
        assertTrue(out.get("updated").asBoolean());
        assertEquals(4, out.get("set").size());
        assertEquals(1, out.get("removed").size());
        assertEquals("obsolete", out.get("removed").get(0).asText());

        ValueMap persisted = context.resourceResolver().getResource(COMPONENT).getValueMap();
        assertEquals("New", persisted.get("jcr:title", String.class));
        assertEquals(Long.valueOf(7), persisted.get("count", Long.class));
        assertEquals(Boolean.TRUE, persisted.get("active", Boolean.class));
        assertArrayEquals(new String[] { "x", "y" }, persisted.get("tags", String[].class));
        assertFalse(persisted.containsKey("obsolete"));
    }

    @Test
    public void dryRunPreviewsChangesWithoutPersisting() throws Exception {
        buildFixture();

        JsonNode out = new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"dryRun\":true,\"properties\":{"
                + "\"jcr:title\":\"New\",\"obsolete\":null}}"));

        assertTrue(out.get("dryRun").asBoolean());
        assertEquals("jcr:title", out.get("set").get(0).asText());
        assertEquals("obsolete", out.get("removed").get(0).asText());

        ValueMap persisted = context.resourceResolver().getResource(COMPONENT).getValueMap();
        assertEquals("Old", persisted.get("jcr:title", String.class)); // unchanged
        assertTrue(persisted.containsKey("obsolete")); // not removed
    }

    @Test
    public void allowsResourceTypeChangeToResolvableComponent() throws Exception {
        buildFixture();

        new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"properties\":{"
                + "\"sling:resourceType\":\"mysite/components/banner\"}}"));

        assertEquals("mysite/components/banner",
            context.resourceResolver().getResource(COMPONENT).getValueMap().get("sling:resourceType", String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsResourceTypeChangeToUnknownComponent() throws Exception {
        buildFixture();
        new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"properties\":{"
                + "\"sling:resourceType\":\"mysite/components/nothere\"}}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPrimaryTypeChange() throws Exception {
        buildFixture();
        new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"properties\":{\"jcr:primaryType\":\"nt:unstructured\"}}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyProperties() throws Exception {
        buildFixture();
        new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"properties\":{}}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTargetOutsidePageContent() throws Exception {
        buildFixture();
        context.build().resource("/content/mysite/loose", "sling:resourceType", "x/y").commit();
        new UpdateComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/mysite/loose\",\"properties\":{\"jcr:title\":\"New\"}}"));
    }

    @Test
    public void isAuthoringOnlyWriteTool() {
        UpdateComponentTool tool = new UpdateComponentTool();
        assertTrue(tool.writesContent());
        assertTrue(tool.authoringOnly());
        assertEquals("update_component", tool.name());
    }
}
