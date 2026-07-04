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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@code create_specific_pdp}. The {@code createPage} seam is overridden so the test can (a) capture the
 * {@code parentPath}/{@code name}/{@code templatePath}/{@code title} the tool would pass to
 * {@code PageManager.create}, and (b) return a canned page whose {@code jcr:content} {@code sling:resourceType} is
 * set DIRECTLY to a core CIF structure-page type ({@code core/cif/components/structure/page/v3/page}) -- the type
 * {@link BindPageToProductsTool} gates on. Because the pinned aem-mock's {@code Resource#isResourceType(String)}
 * matches by exact identity only and does not walk a Venia proxy's {@code /apps} super-type chain the way real AEM
 * does, the delegated {@link BindPageToProductsTool}'s gate would otherwise reject the freshly-created page. The
 * Venia proxy path is proven live, not in aem-mock (see {@code PageTemplateSupport}'s "aem-mock limitation" caveat).
 */
public class CreateSpecificPdpToolTest {

    // The core CIF structure page type BindPageToProductsTool gates on (redeclared from that tool).
    private static final String CORE_STRUCTURE_PAGE_TYPE = "core/cif/components/structure/page/v3/page";

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

    /** Records the create-seam invocation and materialises a canned core-typed structure page in the mock JCR. */
    private static class RecordingPdpTool extends CreateSpecificPdpTool {
        String capturedParent;
        String capturedName;
        String capturedTemplate;
        String capturedTitle;
        boolean seamCalled;

        @Override
        protected String createPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
            String title) {
            this.seamCalled = true;
            this.capturedParent = parentPath;
            this.capturedName = name;
            this.capturedTemplate = templatePath;
            this.capturedTitle = title;
            String pagePath = parentPath + "/" + name;
            try {
                Resource parent = resolver.getResource(parentPath);
                Map<String, Object> pageProps = new HashMap<String, Object>();
                pageProps.put("jcr:primaryType", "cq:Page");
                Resource page = resolver.create(parent, name, pageProps);
                Map<String, Object> contentProps = new HashMap<String, Object>();
                contentProps.put("jcr:primaryType", "cq:PageContent");
                contentProps.put("jcr:title", title);
                contentProps.put("sling:resourceType", CORE_STRUCTURE_PAGE_TYPE);
                resolver.create(page, "jcr:content", contentProps);
            } catch (PersistenceException e) {
                throw new IllegalStateException(e);
            }
            return pagePath;
        }
    }

    private void loadTemplates() {
        context.load().json("/context/conf-templates.json", "/conf");
    }

    @Test
    public void writesContent() {
        assertTrue(new CreateSpecificPdpTool().writesContent());
    }

    @Test
    public void createsProductPageAndBindsProducts() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPdpTool tool = new RecordingPdpTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\",\"sku-2\"]}"));

        // Seam called with the resolved product template + derived args.
        assertTrue(tool.seamCalled);
        assertEquals("/content/site/en", tool.capturedParent);
        assertEquals("my-shoe", tool.capturedName);
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", tool.capturedTemplate);
        assertEquals("My Shoe", tool.capturedTitle);

        // Result shape.
        assertEquals("/content/site/en/my-shoe", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", out.get("template").asText());
        assertFalse(out.get("dryRun").asBoolean());
        assertEquals("sku-1", out.get("boundSkus").get(0).asText());
        assertEquals("sku-2", out.get("boundSkus").get(1).asText());

        // The delegated binding persisted selectorFilter (plain SKUs) on the new page's jcr:content (readback).
        Resource content = context.resourceResolver().getResource("/content/site/en/my-shoe/jcr:content");
        String[] filter = content.getValueMap().get("selectorFilter", String[].class);
        assertEquals(2, filter.length);
        assertEquals("sku-1", filter[0]);
        assertEquals("sku-2", filter[1]);
    }

    @Test
    public void derivesUniqueNameFromTitleWhenNameOmitted() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPdpTool tool = new RecordingPdpTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"title\":\"My Shoe\",\"skusOrUrlKeys\":[\"sku-1\"]}"));

        assertTrue(tool.capturedName != null && !tool.capturedName.isEmpty());
        assertEquals("/content/site/en/" + tool.capturedName, out.get("pagePath").asText());
    }

    @Test
    public void dryRunCreatesNothing() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        final ResourceResolver real = context.resourceResolver();
        ResourceResolver spyResolver = mock(ResourceResolver.class);
        when(spyResolver.getResource(org.mockito.Mockito.anyString()))
            .thenAnswer(inv -> real.getResource((String) inv.getArguments()[0]));

        RecordingPdpTool tool = new RecordingPdpTool();
        JsonNode out = tool.call(ctx(spyResolver), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\"],\"dryRun\":true}"));

        // Neither the create seam nor any commit ran.
        assertFalse(tool.seamCalled);
        org.mockito.Mockito.verify(spyResolver, org.mockito.Mockito.never()).commit();

        // Would-be path + template still computed and returned.
        assertEquals("/content/site/en/my-shoe", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", out.get("template").asText());
        assertTrue(out.get("dryRun").asBoolean());
        assertEquals("sku-1", out.get("boundSkus").get(0).asText());

        // Nothing was actually created.
        assertNull(real.getResource("/content/site/en/my-shoe"));
    }

    @Test
    public void rejectsParentNotUnderContent() {
        loadTemplates();
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/conf/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\"]}")));
    }

    @Test
    public void rejectsMissingParent() {
        loadTemplates();
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/does/not/exist\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\"]}")));
    }

    @Test
    public void rejectsEmptySkusOrUrlKeys() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // Empty array -> IAE (a PDP must bind at least one product).
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\",\"skusOrUrlKeys\":[]}")));
        // Array of only blank entries -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\",\"skusOrUrlKeys\":[\"  \"]}")));
        // Missing entirely -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\"}")));
    }

    @Test
    public void rejectsEmptySkusOrUrlKeysEvenOnDryRun() {
        // dryRun must faithfully preview a real run: a missing/empty required arg (which a real run rejects) must
        // also be rejected here (required-arg validation precedes the dryRun branch).
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\",\"skusOrUrlKeys\":[],"
                + "\"dryRun\":true}")));
    }

    @Test
    public void rejectsMissingTitle() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"skusOrUrlKeys\":[\"sku-1\"]}")));
    }

    @Test
    public void rejectsExplicitTemplateThatIsNotProduct() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // category-page classifies as "category", not "product" -> IAE before any create.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\"],"
                + "\"template\":\"/conf/testsite/settings/wcm/templates/category-page\"}")));
    }

    @Test
    public void surfacesBindingFailureWhenReadbackReportsNotUpdated() throws Exception {
        // The delegate reports updated=false (write did not persist) WITHOUT throwing -> create must fail closed.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        CreateSpecificPdpTool tool = new RecordingPdpTool() {
            @Override
            protected JsonNode bindProducts(McpCallContext c, ObjectNode bindArgs) {
                return mapper.createObjectNode().put("updated", false);
            }
        };

        assertThrows(IllegalStateException.class, () -> tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\"]}")));
    }

    @Test
    public void revertsStagedPageWhenBindingThrows() throws Exception {
        // If the binding fails before its commit, the staged page must be reverted -- no orphaned page left behind.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        ResourceResolver spy = org.mockito.Mockito.spy(context.resourceResolver());
        CreateSpecificPdpTool tool = new RecordingPdpTool() {
            @Override
            protected JsonNode bindProducts(McpCallContext c, ObjectNode bindArgs) {
                throw new IllegalArgumentException("resource is not a CIF structure page");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx(spy), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"my-shoe\",\"title\":\"My Shoe\","
                + "\"skusOrUrlKeys\":[\"sku-1\"]}")));

        org.mockito.Mockito.verify(spy).revert();
    }
}
