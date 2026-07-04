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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@code create_specific_pdp_for_category_tree}. The {@code createPage} seam is overridden so the test can
 * (a) capture the {@code parentPath}/{@code name}/{@code templatePath}/{@code title} the tool would pass to
 * {@code PageManager.create}, and (b) return a canned page whose {@code jcr:content} {@code sling:resourceType} is set
 * DIRECTLY to a core CIF structure-page type ({@code core/cif/components/structure/page/v3/page}) -- the type
 * {@link BindProductPageToCategoryTreeTool} gates on. Because the pinned aem-mock's {@code Resource#isResourceType(
 * String)} matches by exact identity only and does not walk a Venia proxy's {@code /apps} super-type chain the way real
 * AEM does, the delegated {@link BindProductPageToCategoryTreeTool}'s gate would otherwise reject the freshly-created
 * page. The Venia proxy path is proven live, not in aem-mock (see {@code PageTemplateSupport}'s "aem-mock limitation"
 * caveat).
 */
public class CreateSpecificPdpForCategoryTreeToolTest {

    // The core CIF structure page type BindProductPageToCategoryTreeTool gates on (redeclared from that tool).
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
    private static class RecordingPdpTreeTool extends CreateSpecificPdpForCategoryTreeTool {
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
        assertTrue(new CreateSpecificPdpForCategoryTreeTool().writesContent());
    }

    @Test
    public void createsProductPageAndBindsCategoryTree() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPdpTreeTool tool = new RecordingPdpTreeTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\",\"includesSubCategories\":true}"));

        // Seam called with the resolved product template + derived args.
        assertTrue(tool.seamCalled);
        assertEquals("/content/site/en", tool.capturedParent);
        assertEquals("tops-pdp", tool.capturedName);
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", tool.capturedTemplate);
        assertEquals("Tops PDP", tool.capturedTitle);

        // Result shape.
        assertEquals("/content/site/en/tops-pdp", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", out.get("template").asText());
        assertEquals("MjA=", out.get("categoryUid").asText());
        assertEquals("venia-tops", out.get("urlPath").asText());
        assertFalse(out.get("dryRun").asBoolean());

        // The delegated binding persisted the plain-urlPath useForCategories + includesSubCategories on the new
        // page's jcr:content (readback). NOT the pipe-encoded uid|urlPath form -- see BindProductPageToCategoryTreeTool.
        Resource content = context.resourceResolver().getResource("/content/site/en/tops-pdp/jcr:content");
        String[] useForCategories = content.getValueMap().get("useForCategories", String[].class);
        assertEquals(1, useForCategories.length);
        assertEquals("venia-tops", useForCategories[0]);
        assertEquals(Boolean.TRUE, content.getValueMap().get("includesSubCategories", Boolean.class));
    }

    @Test
    public void defaultsIncludesSubCategoriesToFalse() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPdpTreeTool tool = new RecordingPdpTreeTool();
        tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}"));

        Resource content = context.resourceResolver().getResource("/content/site/en/tops-pdp/jcr:content");
        assertEquals(Boolean.FALSE, content.getValueMap().get("includesSubCategories", Boolean.class));
    }

    @Test
    public void derivesUniqueNameFromTitleWhenNameOmitted() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPdpTreeTool tool = new RecordingPdpTreeTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"title\":\"My Tops PDP\",\"categoryUid\":\"MjA=\","
                + "\"urlPath\":\"venia-tops\"}"));

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

        RecordingPdpTreeTool tool = new RecordingPdpTreeTool();
        JsonNode out = tool.call(ctx(spyResolver), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\",\"dryRun\":true}"));

        // Neither the create seam nor any commit ran.
        assertFalse(tool.seamCalled);
        org.mockito.Mockito.verify(spyResolver, org.mockito.Mockito.never()).commit();

        // Would-be path + template still computed and returned.
        assertEquals("/content/site/en/tops-pdp", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/product-page", out.get("template").asText());
        assertEquals("MjA=", out.get("categoryUid").asText());
        assertEquals("venia-tops", out.get("urlPath").asText());
        assertTrue(out.get("dryRun").asBoolean());

        // Nothing was actually created.
        assertNull(real.getResource("/content/site/en/tops-pdp"));
    }

    @Test
    public void rejectsParentNotUnderContent() {
        loadTemplates();
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/conf/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}")));
    }

    @Test
    public void rejectsMissingParent() {
        loadTemplates();
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/does/not/exist\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}")));
    }

    @Test
    public void rejectsMissingCategoryUidOrUrlPath() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // Missing categoryUid -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"urlPath\":\"venia-tops\"}")));
        // Missing urlPath -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\",\"categoryUid\":\"MjA=\"}")));
        // Blank categoryUid -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"  \",\"urlPath\":\"venia-tops\"}")));
    }

    @Test
    public void rejectsMissingCategoryUidOrUrlPathEvenOnDryRun() {
        // dryRun must faithfully preview a real run: a missing required arg (which a real run rejects) must also be
        // rejected here (required-arg validation precedes the dryRun branch).
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\",\"urlPath\":\"venia-tops\","
                + "\"dryRun\":true}")));
    }

    @Test
    public void rejectsMissingTitle() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}")));
    }

    @Test
    public void rejectsExplicitTemplateThatIsNotProduct() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // category-page classifies as "category", not "product" -> IAE before any create.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPdpTreeTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\","
                + "\"template\":\"/conf/testsite/settings/wcm/templates/category-page\"}")));
    }

    @Test
    public void surfacesBindingFailureWhenReadbackReportsNotUpdated() throws Exception {
        // The delegate reports updated=false (write did not persist) WITHOUT throwing -> create must fail closed.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        CreateSpecificPdpForCategoryTreeTool tool = new RecordingPdpTreeTool() {
            @Override
            protected JsonNode bindCategoryTree(McpCallContext c, ObjectNode bindArgs) {
                return mapper.createObjectNode().put("updated", false);
            }
        };

        assertThrows(IllegalStateException.class, () -> tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}")));
    }

    @Test
    public void revertsStagedPageWhenBindingThrows() throws Exception {
        // If the binding fails before its commit, the staged page must be reverted -- no orphaned page left behind.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        ResourceResolver spy = org.mockito.Mockito.spy(context.resourceResolver());
        CreateSpecificPdpForCategoryTreeTool tool = new RecordingPdpTreeTool() {
            @Override
            protected JsonNode bindCategoryTree(McpCallContext c, ObjectNode bindArgs) {
                throw new IllegalArgumentException("resource is not a CIF structure page");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx(spy), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"tops-pdp\",\"title\":\"Tops PDP\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"venia-tops\"}")));

        org.mockito.Mockito.verify(spy).revert();
    }
}
