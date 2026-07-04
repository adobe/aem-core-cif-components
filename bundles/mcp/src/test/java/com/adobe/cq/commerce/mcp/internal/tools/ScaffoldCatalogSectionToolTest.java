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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@code scaffold_catalog_section}. Both composition seams are overridden:
 * <ul>
 * <li>{@code createCatalogRoot} -- materialises a canned catalog section-root page and returns a canned
 * {@code {pagePath, ...}} result, standing in for the delegated {@link CreateCatalogPageTool} (whose own delegated
 * {@code configure_catalog_page} gate can't pass in the pinned aem-mock -- it can't resolve Venia's proxy
 * super-typing; see {@code PageTemplateSupport}'s aem-mock caveat).</li>
 * <li>{@code createChildPage} -- materialises canned child pages and returns their paths, standing in for
 * {@link com.adobe.cq.commerce.mcp.internal.PageCreationSupport#createPage} (the pinned aem-mock's
 * {@code PageManager} works, but keeping the seam overridden mirrors the sibling create tools and keeps the test
 * independent of proxy resolution).</li>
 * </ul>
 */
public class ScaffoldCatalogSectionToolTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext ctx() {
        return ctx(context.resourceResolver());
    }

    private StoreContext ctx(ResourceResolver resolver) {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(resolver);
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    private void loadTemplates() {
        context.load().json("/context/conf-templates.json", "/conf");
    }

    private void loadCatalogOnlyTemplates() {
        context.load().json("/context/conf-templates-catalog-only.json", "/conf");
    }

    /**
     * Materialises canned pages for both composition seams so the test never depends on Venia proxy resolution.
     * {@code createCatalogRoot} creates the section-root catalog page and echoes a canned result; {@code createChildPage}
     * creates the example child pages via a plain mock-JCR write.
     */
    private class RecordingScaffoldTool extends ScaffoldCatalogSectionTool {
        boolean catalogRootCalled;
        ObjectNode capturedCatalogArgs;

        @Override
        protected JsonNode createCatalogRoot(McpCallContext c, ObjectNode args) {
            this.catalogRootCalled = true;
            this.capturedCatalogArgs = args;
            ResourceResolver resolver = ((StoreContext) c).getRequest().getResourceResolver();
            String parent = args.get("parent").asText();
            String name = args.get("name").asText();
            String pagePath = parent + "/" + name;
            if (!args.path("dryRun").asBoolean(false)) {
                materializePage(resolver, parent, name, args.path("title").asText(name));
            }
            ObjectNode result = mapper.createObjectNode();
            result.put("pagePath", pagePath);
            result.put("template", "/conf/testsite/settings/wcm/templates/catalog-page");
            result.put("rootCategoryId", args.path("rootCategoryId").asText());
            result.put("idType", args.path("idType").asText("uid"));
            result.put("dryRun", args.path("dryRun").asBoolean(false));
            return result;
        }

        @Override
        protected String createChildPage(ResourceResolver resolver, String parentPath, String name,
            String templatePath, String title) {
            materializePage(resolver, parentPath, name, title);
            return parentPath + "/" + name;
        }

        private void materializePage(ResourceResolver resolver, String parentPath, String name, String title) {
            try {
                Resource parent = resolver.getResource(parentPath);
                Map<String, Object> pageProps = new HashMap<String, Object>();
                pageProps.put("jcr:primaryType", "cq:Page");
                Resource page = resolver.create(parent, name, pageProps);
                Map<String, Object> contentProps = new HashMap<String, Object>();
                contentProps.put("jcr:primaryType", "cq:PageContent");
                contentProps.put("jcr:title", title);
                resolver.create(page, "jcr:content", contentProps);
            } catch (PersistenceException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Test
    public void writesContent() {
        assertTrue(new ScaffoldCatalogSectionTool().writesContent());
    }

    @Test
    public void scaffoldsCatalogSectionWithBothExampleChildren() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingScaffoldTool tool = new RecordingScaffoldTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}"));

        // Catalog root delegated with the right args (title defaults, idType default uid).
        assertTrue(tool.catalogRootCalled);
        assertEquals("/content/site/en", tool.capturedCatalogArgs.get("parent").asText());
        assertEquals("shop", tool.capturedCatalogArgs.get("name").asText());
        assertEquals("MjA=", tool.capturedCatalogArgs.get("rootCategoryId").asText());
        assertEquals("uid", tool.capturedCatalogArgs.get("idType").asText());

        // Result shape.
        assertEquals("/content/site/en/shop", out.get("sectionPath").asText());
        assertEquals("/content/site/en/shop", out.get("catalogPage").asText());
        assertEquals("MjA=", out.get("rootCategoryId").asText());
        assertFalse(out.get("dryRun").asBoolean());

        // Two children, one product + one category, created under the section root.
        JsonNode children = out.get("children");
        assertEquals(2, children.size());
        assertEquals("/content/site/en/shop/example-product", children.get(0).get("path").asText());
        assertEquals("product", children.get(0).get("pageType").asText());
        assertEquals("/content/site/en/shop/example-category", children.get(1).get("path").asText());
        assertEquals("category", children.get(1).get("pageType").asText());
        assertEquals(0, out.get("skipped").size());

        // Readback: the section root + both children actually exist.
        assertNotNull(context.resourceResolver().getResource("/content/site/en/shop"));
        assertNotNull(context.resourceResolver().getResource("/content/site/en/shop/example-product"));
        assertNotNull(context.resourceResolver().getResource("/content/site/en/shop/example-category"));
    }

    @Test
    public void titleDefaultsToNameWhenOmitted() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingScaffoldTool tool = new RecordingScaffoldTool();
        tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"rootCategoryId\":\"MjA=\"}"));

        assertEquals("shop", tool.capturedCatalogArgs.get("title").asText());
    }

    @Test
    public void gracefullySkipsChildWhenTemplateAbsent() throws Exception {
        // Catalog-only fixture: no product/category template -> both children skipped, catalog page still created.
        loadCatalogOnlyTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingScaffoldTool tool = new RecordingScaffoldTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}"));

        // Catalog root still created (no throw).
        assertTrue(tool.catalogRootCalled);
        assertEquals("/content/site/en/shop", out.get("catalogPage").asText());
        assertNotNull(context.resourceResolver().getResource("/content/site/en/shop"));

        // No children created; both skips recorded.
        assertEquals(0, out.get("children").size());
        JsonNode skipped = out.get("skipped");
        assertEquals(2, skipped.size());
        assertEquals("product", skipped.get(0).get("pageType").asText());
        assertNotNull(skipped.get(0).get("reason"));
        assertEquals("category", skipped.get(1).get("pageType").asText());
        assertNull(context.resourceResolver().getResource("/content/site/en/shop/example-product"));
    }

    @Test
    public void dryRunCreatesNothing() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        final ResourceResolver real = context.resourceResolver();
        ResourceResolver spyResolver = mock(ResourceResolver.class);
        when(spyResolver.getResource(org.mockito.Mockito.anyString()))
            .thenAnswer(inv -> real.getResource((String) inv.getArguments()[0]));

        RecordingScaffoldTool tool = new RecordingScaffoldTool();
        JsonNode out = tool.call(ctx(spyResolver), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"dryRun\":true}"));

        // No commit ever ran.
        verify(spyResolver, never()).commit();

        // The catalog-root delegate was called with dryRun:true.
        assertTrue(tool.catalogRootCalled);
        assertTrue(tool.capturedCatalogArgs.get("dryRun").asBoolean());

        // Full would-be tree returned.
        assertTrue(out.get("dryRun").asBoolean());
        assertEquals("/content/site/en/shop", out.get("sectionPath").asText());
        JsonNode children = out.get("children");
        assertEquals(2, children.size());
        assertEquals("/content/site/en/shop/example-product", children.get(0).get("path").asText());
        assertEquals("/content/site/en/shop/example-category", children.get(1).get("path").asText());

        // Nothing actually created.
        assertNull(real.getResource("/content/site/en/shop/example-product"));
    }

    @Test
    public void dryRunRecordsSkippedForAbsentTemplates() throws Exception {
        loadCatalogOnlyTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingScaffoldTool tool = new RecordingScaffoldTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"dryRun\":true}"));

        assertTrue(out.get("dryRun").asBoolean());
        assertEquals(0, out.get("children").size());
        assertEquals(2, out.get("skipped").size());
    }

    @Test
    public void revertsChildrenWhenAChildCreateThrows() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        ResourceResolver spy = spy(context.resourceResolver());
        ScaffoldCatalogSectionTool tool = new RecordingScaffoldTool() {
            @Override
            protected String createChildPage(ResourceResolver resolver, String parentPath, String name,
                String templatePath, String title) {
                throw new IllegalStateException("child create blew up");
            }
        };

        assertThrows(IllegalStateException.class, () -> tool.call(ctx(spy), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));

        // The staged children were reverted (the already-committed catalog root is a best-effort boundary).
        verify(spy).revert();
    }

    @Test
    public void rejectsParentNotUnderContent() {
        loadTemplates();
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingScaffoldTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/conf/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));
    }

    @Test
    public void rejectsMissingParent() {
        loadTemplates();
        assertThrows(IllegalArgumentException.class, () -> new RecordingScaffoldTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/does/not/exist\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));
    }

    @Test
    public void rejectsMissingName() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingScaffoldTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));
    }

    @Test
    public void rejectsMissingRootCategoryId() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingScaffoldTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\"}")));
    }

    @Test
    public void rejectsInvalidIdType() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingScaffoldTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"idType\":\"bogus\"}")));
    }
}
