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
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoveComponentToolTest {

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
            .resource("/content/mysite/en", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent",
                "sling:resourceType", "mysite/components/page")
            .resource("root", "sling:resourceType", "wcm/foundation/components/responsivegrid")
            .resource("teaser", "sling:resourceType", "mysite/components/teaser")
            .resource("child", "sling:resourceType", "mysite/components/text")
            .commit();
    }

    @Test
    public void removesComponentSubtreeAndVerifies() throws Exception {
        buildFixture();

        JsonNode out = new RemoveComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\"}"));

        assertFalse(out.get("dryRun").asBoolean());
        assertTrue(out.get("removed").asBoolean());
        assertEquals(COMPONENT, out.get("path").asText());
        assertEquals("mysite/components/teaser", out.get("resourceType").asText());
        assertNull(context.resourceResolver().getResource(COMPONENT));
        assertNull(context.resourceResolver().getResource(COMPONENT + "/child")); // subtree gone too
    }

    @Test
    public void dryRunPreviewsRemovalWithoutPersisting() throws Exception {
        buildFixture();

        JsonNode out = new RemoveComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "\",\"dryRun\":true}"));

        assertTrue(out.get("dryRun").asBoolean());
        assertFalse(out.get("removed").asBoolean());
        assertNotNull(context.resourceResolver().getResource(COMPONENT)); // still there
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPagePath() throws Exception {
        buildFixture();
        new RemoveComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/mysite/en\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPageContentNodeItself() throws Exception {
        buildFixture();
        new RemoveComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/mysite/en/jcr:content\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingComponent() throws Exception {
        buildFixture();
        new RemoveComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + COMPONENT + "-nothere\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathOutsideContent() throws Exception {
        buildFixture();
        context.build().resource("/apps/mysite/components/teaser", "jcr:primaryType", "cq:Component").commit();
        new RemoveComponentTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/apps/mysite/components/teaser\"}"));
    }

    @Test
    public void isAuthoringOnlyWriteTool() {
        RemoveComponentTool tool = new RemoveComponentTool();
        assertTrue(tool.writesContent());
        assertTrue(tool.authoringOnly());
        assertEquals("remove_component", tool.name());
    }
}
