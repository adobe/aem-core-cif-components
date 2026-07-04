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
 * Tests for {@code create_catalog_page}. The {@code createPage} seam is overridden so the test can (a) capture the
 * {@code parentPath}/{@code name}/{@code templatePath}/{@code title} the tool would pass to
 * {@code PageManager.create}, and (b) return a canned page whose {@code jcr:content} {@code sling:resourceType} is
 * set DIRECTLY to a core catalog-page type ({@code core/cif/components/structure/catalogpage/v3/catalogpage}) --
 * because the pinned aem-mock's {@code Resource#isResourceType(String)} matches by exact identity only and does not
 * walk a Venia proxy's {@code /apps} super-type chain the way real AEM does, the delegated
 * {@link ConfigureCatalogPageTool}'s gate would otherwise reject the freshly-created page. The Venia proxy path is
 * proven live, not in aem-mock (see {@code SuggestTemplateForPageTypeTool}'s "aem-mock limitation" caveat).
 */
public class CreateCatalogPageToolTest {

    private static final String CORE_CATALOG_TYPE = "core/cif/components/structure/catalogpage/v3/catalogpage";

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

    /** Records the create-seam invocation and materialises a canned core-typed catalog page in the mock JCR. */
    private static class RecordingCatalogPageTool extends CreateCatalogPageTool {
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
                contentProps.put("sling:resourceType", CORE_CATALOG_TYPE);
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
        assertTrue(new CreateCatalogPageTool().writesContent());
    }

    @Test
    public void createsCatalogPageAndBindsRootCategory() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingCatalogPageTool tool = new RecordingCatalogPageTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}"));

        // Seam called with the resolved catalog template + derived args.
        assertTrue(tool.seamCalled);
        assertEquals("/content/site/en", tool.capturedParent);
        assertEquals("shop", tool.capturedName);
        assertEquals("/conf/testsite/settings/wcm/templates/catalog-page", tool.capturedTemplate);
        assertEquals("Shop", tool.capturedTitle);

        // Result shape.
        assertEquals("/content/site/en/shop", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/catalog-page", out.get("template").asText());
        assertEquals("MjA=", out.get("rootCategoryId").asText());
        assertEquals("uid", out.get("idType").asText());
        assertFalse(out.get("dryRun").asBoolean());

        // The delegated binding persisted the root-category props on the new page's jcr:content (readback).
        Resource content = context.resourceResolver().getResource("/content/site/en/shop/jcr:content");
        assertEquals("MjA=", content.getValueMap().get("magentoRootCategoryId", String.class));
        assertEquals("uid", content.getValueMap().get("magentoRootCategoryIdType", String.class));
        assertEquals(Boolean.FALSE, content.getValueMap().get("showMainCategories", Boolean.class));
    }

    @Test
    public void honoursIdTypeAndShowMainCategories() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingCatalogPageTool tool = new RecordingCatalogPageTool();
        tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"cat/1\","
                + "\"idType\":\"urlPath\",\"showMainCategories\":true}"));

        Resource content = context.resourceResolver().getResource("/content/site/en/shop/jcr:content");
        assertEquals("cat/1", content.getValueMap().get("magentoRootCategoryId", String.class));
        assertEquals("urlPath", content.getValueMap().get("magentoRootCategoryIdType", String.class));
        assertEquals(Boolean.TRUE, content.getValueMap().get("showMainCategories", Boolean.class));
    }

    @Test
    public void derivesUniqueNameFromTitleWhenNameOmitted() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        RecordingCatalogPageTool tool = new RecordingCatalogPageTool();
        JsonNode out = tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"title\":\"My Shop\",\"rootCategoryId\":\"MjA=\"}"));

        // A name was derived (non-blank) and used for both the seam and the result path.
        assertTrue(tool.capturedName != null && !tool.capturedName.isEmpty());
        assertEquals("/content/site/en/" + tool.capturedName, out.get("pagePath").asText());
    }

    @Test
    public void dryRunCreatesNothing() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // A stand-in resolver that delegates reads to the real mock JCR but records whether commit() is ever
        // called -- dryRun must never commit.
        final ResourceResolver real = context.resourceResolver();
        ResourceResolver spyResolver = mock(ResourceResolver.class);
        when(spyResolver.getResource(org.mockito.Mockito.anyString()))
            .thenAnswer(inv -> real.getResource((String) inv.getArguments()[0]));

        RecordingCatalogPageTool tool = new RecordingCatalogPageTool();
        JsonNode out = tool.call(ctx(spyResolver), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"dryRun\":true}"));

        // Neither the create seam nor any commit ran.
        assertFalse(tool.seamCalled);
        org.mockito.Mockito.verify(spyResolver, org.mockito.Mockito.never()).commit();

        // Would-be path + template still computed and returned.
        assertEquals("/content/site/en/shop", out.get("pagePath").asText());
        assertEquals("/conf/testsite/settings/wcm/templates/catalog-page", out.get("template").asText());
        assertTrue(out.get("dryRun").asBoolean());

        // Nothing was actually created.
        assertNull(real.getResource("/content/site/en/shop"));
    }

    @Test
    public void rejectsParentNotUnderContent() {
        loadTemplates();
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/conf/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));
    }

    @Test
    public void rejectsMissingParent() {
        loadTemplates();
        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/does/not/exist\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));
    }

    @Test
    public void rejectsExplicitTemplateThatIsNotCatalog() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        // product-page classifies as "product", not "catalog" -> IAE before any create.
        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"template\":\"/conf/testsite/settings/wcm/templates/product-page\"}")));
    }

    @Test
    public void rejectsInvalidIdType() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"idType\":\"bogus\"}")));
    }

    @Test
    public void rejectsInvalidIdTypeEvenOnDryRun() {
        // dryRun must faithfully preview a real run: an invalid idType (which a real run rejects) must also fail here.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\","
                + "\"idType\":\"bogus\",\"dryRun\":true}")));
    }

    @Test
    public void dryRunDerivesNameWhenOmitted() throws Exception {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        JsonNode out = new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"title\":\"My Shop\",\"rootCategoryId\":\"MjA=\",\"dryRun\":true}"));

        // Even on dryRun the would-be path uses the real derived (non-blank) child name under the parent.
        assertTrue(out.get("dryRun").asBoolean());
        String pagePath = out.get("pagePath").asText();
        assertTrue(pagePath, pagePath.startsWith("/content/site/en/") && pagePath.length() > "/content/site/en/".length());
    }

    @Test
    public void surfacesBindingFailureWhenReadbackReportsNotUpdated() throws Exception {
        // The delegate reports updated=false (write did not persist) WITHOUT throwing -> create must fail closed
        // rather than report a successful create over a broken binding.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        CreateCatalogPageTool tool = new RecordingCatalogPageTool() {
            @Override
            protected JsonNode bindRootCategory(McpCallContext c, ObjectNode bindArgs) {
                return mapper.createObjectNode().put("updated", false);
            }
        };

        assertThrows(IllegalStateException.class, () -> tool.call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));
    }

    @Test
    public void revertsStagedPageWhenBindingThrows() throws Exception {
        // If the binding fails before its commit (e.g. the created page's type is rejected by the gate), the staged
        // page must be reverted -- no orphaned, unbound page left behind.
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        ResourceResolver spy = org.mockito.Mockito.spy(context.resourceResolver());
        CreateCatalogPageTool tool = new RecordingCatalogPageTool() {
            @Override
            protected JsonNode bindRootCategory(McpCallContext c, ObjectNode bindArgs) {
                throw new IllegalArgumentException("resource is not a CIF catalog page");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx(spy), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\",\"rootCategoryId\":\"MjA=\"}")));

        // The staged page create was rolled back.
        org.mockito.Mockito.verify(spy).revert();
    }

    @Test
    public void requiresTitleAndRootCategoryId() {
        loadTemplates();
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();

        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"rootCategoryId\":\"MjA=\"}")));
        assertThrows(IllegalArgumentException.class, () -> new RecordingCatalogPageTool().call(ctx(), mapper.readTree(
            "{\"parent\":\"/content/site/en\",\"name\":\"shop\",\"title\":\"Shop\"}")));
    }
}
