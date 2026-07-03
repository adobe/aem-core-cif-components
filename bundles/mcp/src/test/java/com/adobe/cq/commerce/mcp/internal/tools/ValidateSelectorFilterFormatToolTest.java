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
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidateSelectorFilterFormatToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private void load() {
        context.load().json("/context/specific-pages.json", "/content");
    }

    private StoreContext ctx() {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    private JsonNode call(String path) {
        StoreContext ctx = ctx();
        return new ValidateSelectorFilterFormatTool().call(ctx, mapper.createObjectNode().put("path", path));
    }

    @Test
    public void wellFormedEntryIsValidWithUidAndUrlPath() {
        load();

        JsonNode out = call("/content/site/category-page/sub-page");

        assertEquals("/content/site/category-page/sub-page", out.get("path").asText());
        assertEquals("uidAndUrlPath", out.get("selectorFilterType").asText());

        JsonNode entries = out.get("entries");
        assertTrue(entries.isArray());
        assertEquals(1, entries.size());

        JsonNode entry = entries.get(0);
        assertEquals("category-uid-1|men/tops", entry.get("raw").asText());
        assertTrue(entry.get("valid").asBoolean());
        assertEquals("category-uid-1", entry.get("uid").asText());
        assertEquals("men/tops", entry.get("urlPath").asText());
        assertFalse(entry.has("issue"));
    }

    @Test
    public void pipeLessEntryIsInvalidWithIssueAndNoUidOrUrlPath() {
        load();

        JsonNode out = call("/content/site/category-page/malformed-page");

        assertEquals("/content/site/category-page/malformed-page", out.get("path").asText());
        assertEquals("uidAndUrlPath", out.get("selectorFilterType").asText());

        JsonNode entries = out.get("entries");
        assertEquals(2, entries.size());

        // real fixture values traced through: first entry well-formed, second pipe-less
        JsonNode wellFormed = entries.get(0);
        assertEquals("category-uid-3|women/women-tops", wellFormed.get("raw").asText());
        assertTrue(wellFormed.get("valid").asBoolean());
        assertEquals("category-uid-3", wellFormed.get("uid").asText());
        assertEquals("women/women-tops", wellFormed.get("urlPath").asText());
        assertFalse(wellFormed.has("issue"));

        JsonNode malformed = entries.get(1);
        assertEquals("no-pipe-here", malformed.get("raw").asText());
        assertFalse(malformed.get("valid").asBoolean());
        assertEquals("missing '|' separator", malformed.get("issue").asText());
        assertFalse(malformed.has("uid"));
        assertFalse(malformed.has("urlPath"));
    }

    @Test
    public void pageWithNoSelectorFilterHasEmptyEntries() {
        load();

        JsonNode out = call("/content/site/product-page");

        assertEquals("/content/site/product-page", out.get("path").asText());
        JsonNode entries = out.get("entries");
        assertTrue(entries.isArray());
        assertEquals(0, entries.size());
    }

    @Test
    public void failsClosedWhenPathMissing() {
        assertThrows(IllegalArgumentException.class,
            () -> new ValidateSelectorFilterFormatTool().call(ctx(), mapper.createObjectNode()));
    }

    @Test
    public void failsClosedWhenPathBlank() {
        assertThrows(IllegalArgumentException.class, () -> call("   "));
    }

    @Test
    public void failsClosedWhenPathNotUnderContent() {
        assertThrows(IllegalArgumentException.class, () -> call("/etc/somewhere"));
    }

    @Test
    public void failsClosedWhenResourceNotFound() {
        load();
        assertThrows(IllegalArgumentException.class, () -> call("/content/site/does-not-exist"));
    }

    @Test
    public void failsClosedWhenResourceNotAPage() {
        load();
        context.build().resource("/content/not-a-page").commit();
        assertThrows(IllegalArgumentException.class, () -> call("/content/not-a-page"));
    }
}
