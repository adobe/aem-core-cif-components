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
 * Tests for {@code create_specific_plp}. The {@code createPage} seam is overridden so the test can (a) capture the
 * {@code parentPath}/{@code name}/{@code templatePath}/{@code title} the tool would pass to
 * {@code PageManager.create}, and (b) return a canned page whose {@code jcr:content} {@code sling:resourceType} is
 * set DIRECTLY to a core CIF structure-page type ({@code core/cif/components/structure/page/v3/page}) -- the type
 * {@link BindPageToCategoryTool} gates on. Because the pinned aem-mock's {@code Resource#isResourceType(String)}
 * matches by exact identity only and does not walk a Venia proxy's {@code /apps} super-type chain the way real AEM
 * does, the delegated {@link BindPageToCategoryTool}'s gate would otherwise reject the freshly-created page. The
 * Venia proxy path is proven live, not in aem-mock (see {@code PageTemplateSupport}'s "aem-mock limitation" caveat).
 */
public class CreateSpecificPlpToolTest {

    // The core CIF structure page type BindPageToCategoryTool gates on (redeclared from that tool).
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
    private static class RecordingPlpTool extends CreateSpecificPlpTool {
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
        assertTrue(new CreateSpecificPlpTool().writesContent());
    }

    @Test
    public void createsCategoryPageAndBindsCategory() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPlpTool tool = new RecordingPlpTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\",\"includesSubCategories\":true}"));

        // Seam called with the resolved category template + derived args.
        assertTrue(tool.seamCalled);
        assertEquals("/content/site/en", tool.capturedParent);
        assertEquals("shoes", tool.capturedName);
        assertEquals("/conf/testsite/settings/wcm/templates/category-page", tool.capturedTemplate);
        assertEquals("Shoes", tool.capturedTitle);

        // Result shape.
        assertEquals("/content/site/en/shoes", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/category-page", out.get("template").asText());
        assertEquals("MjA=", out.get("categoryUid").asText());
        assertEquals("men/shoes", out.get("urlPath").asText());
        assertFalse(out.get("dryRun").asBoolean());

        // The delegated binding persisted the pipe-encoded selectorFilter on the new page's jcr:content (readback).
        Resource content = context.resourceResolver().getResource("/content/site/en/shoes/jcr:content");
        String[] filter = content.getValueMap().get("selectorFilter", String[].class);
        assertEquals(1, filter.length);
        assertEquals("MjA=|men/shoes", filter[0]);
        assertEquals("uidAndUrlPath", content.getValueMap().get("selectorFilterType", String.class));
        assertEquals(Boolean.TRUE, content.getValueMap().get("includesSubCategories", Boolean.class));
    }

    @Test
    public void defaultsIncludesSubCategoriesToFalse() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPlpTool tool = new RecordingPlpTool();
        tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\"}"));

        Resource content = context.resourceResolver().getResource("/content/site/en/shoes/jcr:content");
        assertEquals(Boolean.FALSE, content.getValueMap().get("includesSubCategories", Boolean.class));
    }

    @Test
    public void derivesUniqueNameFromTitleWhenNameOmitted() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingPlpTool tool = new RecordingPlpTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"title\":\"My Shoes\",\"categoryUid\":\"MjA=\","
                + "\"urlPath\":\"men/shoes\"}"));

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

        RecordingPlpTool tool = new RecordingPlpTool();
        JsonNode out = tool.call(ctx(spyResolver), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\",\"dryRun\":true}"));

        // Neither the create seam nor any commit ran.
        assertFalse(tool.seamCalled);
        org.mockito.Mockito.verify(spyResolver, org.mockito.Mockito.never()).commit();

        // Would-be path + template still computed and returned.
        assertEquals("/content/site/en/shoes", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/category-page", out.get("template").asText());
        assertEquals("MjA=", out.get("categoryUid").asText());
        assertEquals("men/shoes", out.get("urlPath").asText());
        assertTrue(out.get("dryRun").asBoolean());

        // Nothing was actually created.
        assertNull(real.getResource("/content/site/en/shoes"));
    }

    @Test
    public void rejectsParentNotUnderContent() {
        loadTemplates();
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/conf/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\"}")));
    }

    @Test
    public void rejectsMissingParent() {
        loadTemplates();
        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/does/not/exist\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\"}")));
    }

    @Test
    public void rejectsMissingCategoryUidOrUrlPath() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // Missing categoryUid -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\",\"urlPath\":\"men/shoes\"}")));
        // Missing urlPath -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\",\"categoryUid\":\"MjA=\"}")));
        // Blank categoryUid -> IAE.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"  \",\"urlPath\":\"men/shoes\"}")));
    }

    @Test
    public void rejectsMissingCategoryUidOrUrlPathEvenOnDryRun() {
        // dryRun must faithfully preview a real run: a missing required arg (which a real run rejects) must also be
        // rejected here (required-arg validation precedes the dryRun branch).
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\",\"urlPath\":\"men/shoes\","
                + "\"dryRun\":true}")));
    }

    @Test
    public void rejectsMissingTitle() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\"}")));
    }

    @Test
    public void rejectsExplicitTemplateThatIsNotCategory() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // product-page classifies as "product", not "category" -> IAE before any create.
        assertThrows(IllegalArgumentException.class, () -> new RecordingPlpTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\","
                + "\"template\":\"/conf/testsite/settings/wcm/templates/product-page\"}")));
    }

    @Test
    public void surfacesBindingFailureWhenReadbackReportsNotUpdated() throws Exception {
        // The delegate reports updated=false (write did not persist) WITHOUT throwing -> create must fail closed.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        CreateSpecificPlpTool tool = new RecordingPlpTool() {
            @Override
            protected JsonNode bindCategory(McpCallContext c, ObjectNode bindArgs) {
                return mapper.createObjectNode().put("updated", false);
            }
        };

        assertThrows(IllegalStateException.class, () -> tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\"}")));
    }

    @Test
    public void revertsStagedPageWhenBindingThrows() throws Exception {
        // If the binding fails before its commit, the staged page must be reverted -- no orphaned page left behind.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        ResourceResolver spy = org.mockito.Mockito.spy(context.resourceResolver());
        CreateSpecificPlpTool tool = new RecordingPlpTool() {
            @Override
            protected JsonNode bindCategory(McpCallContext c, ObjectNode bindArgs) {
                throw new IllegalArgumentException("resource is not a CIF structure page");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx(spy), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shoes\",\"title\":\"Shoes\","
                + "\"categoryUid\":\"MjA=\",\"urlPath\":\"men/shoes\"}")));

        org.mockito.Mockito.verify(spy).revert();
    }
}
