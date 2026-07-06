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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddComponentsToolTest {

    private static final String CONTAINER = "/content/mysite/en/jcr:content/root";

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
            .resource("/apps/mysite/components/teaser",
                "jcr:primaryType", "cq:Component",
                "jcr:title", "Teaser")
            .resource("/content/mysite/en", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent",
                "sling:resourceType", "mysite/components/page")
            .resource("root", "sling:resourceType", "wcm/foundation/components/responsivegrid")
            .commit();
    }

    @Test
    public void addsComponentWithTypedPropertiesAndVerifiesPersistence() throws Exception {
        buildFixture();

        JsonNode out = new AddComponentsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"" + CONTAINER + "\",\"components\":[{"
                + "\"resourceType\":\"mysite/components/teaser\",\"properties\":{"
                + "\"jcr:title\":\"Hello\",\"count\":3,\"active\":true,\"tags\":[\"a\",\"b\"]}}]}"));

        assertFalse(out.get("dryRun").asBoolean());
        assertEquals(1, out.get("created").size());
        String path = out.get("created").get(0).get("path").asText();
        assertEquals(CONTAINER + "/teaser", path); // name derived from the resource type's last segment

        ValueMap persisted = context.resourceResolver().getResource(path).getValueMap();
        assertEquals("mysite/components/teaser", persisted.get("sling:resourceType", String.class));
        assertEquals("Hello", persisted.get("jcr:title", String.class));
        assertEquals(Long.valueOf(3), persisted.get("count", Long.class));
        assertEquals(Boolean.TRUE, persisted.get("active", Boolean.class));
        assertArrayEquals(new String[] { "a", "b" }, persisted.get("tags", String[].class));
    }

    @Test
    public void appendsNumericSuffixOnNameCollision() throws Exception {
        buildFixture();
        context.build().resource(CONTAINER + "/teaser", "sling:resourceType", "mysite/components/teaser").commit();

        JsonNode out = new AddComponentsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"" + CONTAINER + "\",\"components\":[{"
                + "\"resourceType\":\"mysite/components/teaser\"}]}"));

        String path = out.get("created").get(0).get("path").asText();
        assertTrue(path.startsWith(CONTAINER + "/teaser") && !path.equals(CONTAINER + "/teaser"));
        assertEquals("mysite/components/teaser",
            context.resourceResolver().getResource(path).getValueMap().get("sling:resourceType", String.class));
    }

    @Test
    public void dryRunPreviewsDistinctNamesWithoutPersisting() throws Exception {
        buildFixture();

        JsonNode out = new AddComponentsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"" + CONTAINER + "\",\"dryRun\":true,\"components\":["
                + "{\"resourceType\":\"mysite/components/teaser\"},"
                + "{\"resourceType\":\"mysite/components/teaser\"}]}"));

        assertTrue(out.get("dryRun").asBoolean());
        assertEquals(2, out.get("created").size());
        String first = out.get("created").get(0).get("path").asText();
        String second = out.get("created").get(1).get("path").asText();
        assertEquals(CONTAINER + "/teaser", first);
        assertFalse(first.equals(second)); // both previewed names are distinct, like a real run
        assertNull(context.resourceResolver().getResource(first)); // nothing persisted
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsResourceTypeNotResolvingToComponent() throws Exception {
        buildFixture();
        new AddComponentsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"" + CONTAINER + "\",\"components\":[{"
                + "\"resourceType\":\"mysite/components/nothere\"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyComponentsArray() throws Exception {
        buildFixture();
        new AddComponentsTool().call(ctxForResolver(), mapper.readTree(
            "{\"parentPath\":\"" + CONTAINER + "\",\"components\":[]}"));
    }

    @Test
    public void isAuthoringOnlyWriteTool() {
        AddComponentsTool tool = new AddComponentsTool();
        assertTrue(tool.writesContent());
        assertTrue(tool.authoringOnly());
        assertEquals("add_components", tool.name());
    }
}
