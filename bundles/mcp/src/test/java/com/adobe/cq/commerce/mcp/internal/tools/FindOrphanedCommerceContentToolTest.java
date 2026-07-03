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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FindOrphanedCommerceContentTool}.
 * <p>
 * The JCR-SQL2 scan itself ({@link FindOrphanedCommerceContentTool#findTaggedContent}) is exercised via a seam:
 * the pinned aem-mock {@code jcr-mock} 1.3.0's {@code MockQueryManager} never parses or executes a JCR-SQL2
 * statement against the loaded fixture tree -- {@code createQuery(...)} just wraps the statement string, and
 * {@code Query.execute()} walks registered {@code MockQueryResultHandler}s (or falls back to an empty result); there
 * is no real query engine behind it. So a real end-to-end JCR-SQL2 query can't be verified against aem-mock content;
 * only the "match the tagged nodes, read the tag, reduce combined-SKU, check resolution" logic downstream of the
 * query is under test here, using real aem-mock {@link Resource}s loaded from {@code orphaned-content.json} (not
 * mock-echoes) so property values are traced through real {@code ValueMap}s into the JSON output.
 */
public class FindOrphanedCommerceContentToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext ctx() {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    private List<Resource> loadFixtureTaggedResources() {
        context.load().json("/context/orphaned-content.json", "/content");
        ResourceResolver resolver = context.resourceResolver();
        return Arrays.asList(
            resolver.getResource("/content/site/live-product-page/jcr:content"),
            resolver.getResource("/content/site/dead-product-page/jcr:content"),
            resolver.getResource("/content/site/combined-sku-page/jcr:content"),
            resolver.getResource("/content/site/category-page/jcr:content"),
            resolver.getResource("/content/dam/sample.jpg/jcr:content/metadata"));
    }

    @Test
    public void listsOnlyContentWhoseIdentifierNoLongerResolves() {
        List<Resource> tagged = loadFixtureTaggedResources();
        Set<String> resolvingSkus = new HashSet<>(Arrays.asList("24-MB01"));
        Set<String> resolvingUids = new HashSet<>(Arrays.asList("cat1"));

        FindOrphanedCommerceContentTool tool = new FindOrphanedCommerceContentTool() {
            @Override
            protected List<Resource> findTaggedContent(ResourceResolver resolver, String root, int limit) {
                return tagged;
            }

            @Override
            protected boolean productResolves(StoreContext c, String sku) {
                return resolvingSkus.contains(sku);
            }

            @Override
            protected boolean categoryResolves(StoreContext c, String uid) {
                return resolvingUids.contains(uid);
            }
        };

        JsonNode out = tool.call(ctx(), mapper.createObjectNode());

        assertEquals("find_orphaned_commerce_content", tool.name());
        assertEquals("/content", out.get("root").asText());
        assertEquals(5, out.get("scanned").asInt());

        JsonNode orphans = out.get("orphans");
        assertTrue(orphans.isArray());
        // dead-product-page (discontinued-sku), combined-sku-page (base discontinued-sku), category-page
        // (removed-cat), and the DAM asset metadata (discontinued-sku) -- live-product-page (24-MB01) and
        // category-page's live cat1 must NOT appear.
        assertEquals(4, orphans.size());

        boolean sawDeadProduct = false;
        boolean sawCombinedSkuBase = false;
        boolean sawDeadCategory = false;
        boolean sawDeadAsset = false;
        boolean sawLiveProduct = false;
        boolean sawLiveCategory = false;
        for (JsonNode orphan : orphans) {
            String path = orphan.get("path").asText();
            String identifier = orphan.get("identifier").asText();
            String identifierType = orphan.get("identifierType").asText();
            String property = orphan.get("property").asText();

            if ("/content/site/dead-product-page/jcr:content".equals(path)) {
                sawDeadProduct = true;
                assertEquals("discontinued-sku", identifier);
                assertEquals("product", identifierType);
                assertEquals("cq:products", property);
            }
            if ("/content/site/combined-sku-page/jcr:content".equals(path)) {
                sawCombinedSkuBase = true;
                // combined SKU "discontinued-sku#some-variant" must be reduced to its base before resolution/report.
                assertEquals("discontinued-sku", identifier);
                assertEquals("product", identifierType);
                assertEquals("cq:products", property);
            }
            if ("/content/site/category-page/jcr:content".equals(path) && "removed-cat".equals(identifier)) {
                sawDeadCategory = true;
                assertEquals("category", identifierType);
                assertEquals("cq:categories", property);
            }
            if ("/content/dam/sample.jpg/jcr:content/metadata".equals(path)) {
                sawDeadAsset = true;
                assertEquals("discontinued-sku", identifier);
                assertEquals("product", identifierType);
                assertEquals("cq:products", property);
            }
            if ("24-MB01".equals(identifier)) {
                sawLiveProduct = true;
            }
            if ("cat1".equals(identifier)) {
                sawLiveCategory = true;
            }
        }

        assertTrue("expected dead-product-page's discontinued-sku to be reported as orphaned", sawDeadProduct);
        assertTrue("expected combined-sku-page's base SKU to be reported as orphaned", sawCombinedSkuBase);
        assertTrue("expected category-page's removed-cat to be reported as orphaned", sawDeadCategory);
        assertTrue("expected the DAM asset's discontinued-sku to be reported as orphaned", sawDeadAsset);
        assertTrue("live SKU 24-MB01 must never be listed as an orphan", !sawLiveProduct);
        assertTrue("live category cat1 must never be listed as an orphan", !sawLiveCategory);
    }

    @Test
    public void defaultsRootToContentAndLimitTo200() {
        FindOrphanedCommerceContentTool tool = new FindOrphanedCommerceContentTool() {
            @Override
            protected List<Resource> findTaggedContent(ResourceResolver resolver, String root, int limit) {
                assertEquals("/content", root);
                assertEquals(200, limit);
                return java.util.Collections.emptyList();
            }
        };

        JsonNode out = tool.call(ctx(), mapper.createObjectNode());
        assertEquals("/content", out.get("root").asText());
        assertEquals(0, out.get("scanned").asInt());
        assertEquals(0, out.get("orphans").size());
    }

    @Test
    public void honorsExplicitRootAndLimitArgs() {
        FindOrphanedCommerceContentTool tool = new FindOrphanedCommerceContentTool() {
            @Override
            protected List<Resource> findTaggedContent(ResourceResolver resolver, String root, int limit) {
                assertEquals("/content/site", root);
                assertEquals(5, limit);
                return java.util.Collections.emptyList();
            }
        };

        JsonNode args = mapper.createObjectNode().put("root", "/content/site").put("limit", 5);
        JsonNode out = tool.call(ctx(), args);
        assertEquals("/content/site", out.get("root").asText());
    }

    @Test
    public void rootNotUnderContentThrows() {
        FindOrphanedCommerceContentTool tool = new FindOrphanedCommerceContentTool();
        JsonNode args = mapper.createObjectNode().put("root", "/etc/somewhere");
        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx(), args));
    }

    @Test
    public void doesNotOverrideWritesContent() {
        FindOrphanedCommerceContentTool tool = new FindOrphanedCommerceContentTool();
        assertEquals(false, tool.writesContent());
    }
}
