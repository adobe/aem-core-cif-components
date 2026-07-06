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

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class SiteAppsSupportTest {

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();

    private Page pageWithContent(String path, Object... contentProps) {
        context.build().resource(path, "jcr:primaryType", "cq:Page")
            .resource("jcr:content", contentProps).commit();
        return context.resourceResolver().getResource(path).adaptTo(Page.class);
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }

    // --- confPathFor ---

    @Test
    public void confPathDerivedFromTemplateProperty() {
        Page page = pageWithContent("/content/mysite/en",
            "jcr:primaryType", "cq:PageContent",
            "cq:template", "/conf/mysite/settings/wcm/templates/landing-page");
        assertEquals("/conf/mysite", SiteAppsSupport.confPathFor(page));
    }

    @Test
    public void confPathNullWithoutTemplateProperty() {
        Page page = pageWithContent("/content/mysite/en", "jcr:primaryType", "cq:PageContent");
        assertNull(SiteAppsSupport.confPathFor(page));
    }

    @Test
    public void confPathNullForNonEditableTemplatePath() {
        Page page = pageWithContent("/content/mysite/en",
            "jcr:primaryType", "cq:PageContent",
            "cq:template", "/apps/mysite/templates/homepage");
        assertNull(SiteAppsSupport.confPathFor(page));
    }

    @Test
    public void confPathNullForNullPage() {
        assertNull(SiteAppsSupport.confPathFor(null));
    }

    // --- appsComponentsPathFor ---

    @Test
    public void appsPathDerivedFromSiteResourceType() {
        Page page = pageWithContent("/content/venia/us/en",
            "jcr:primaryType", "cq:PageContent",
            "sling:resourceType", "venia/components/structure/page");
        assertEquals("/apps/venia/components", SiteAppsSupport.appsComponentsPathFor(page));
    }

    @Test
    public void appsPathNullForCoreNamespace() {
        Page page = pageWithContent("/content/site/en",
            "jcr:primaryType", "cq:PageContent",
            "sling:resourceType", "core/cif/components/structure/page/v3/page");
        assertNull(SiteAppsSupport.appsComponentsPathFor(page));
    }

    @Test
    public void appsPathNullForAbsoluteResourceType() {
        Page page = pageWithContent("/content/site/en",
            "jcr:primaryType", "cq:PageContent",
            "sling:resourceType", "/apps/venia/components/structure/page");
        assertNull(SiteAppsSupport.appsComponentsPathFor(page));
    }

    @Test
    public void appsPathNullForNullPage() {
        assertNull(SiteAppsSupport.appsComponentsPathFor(null));
    }

    // --- findEditableContainerPath ---

    @Test
    public void findsDeepestEditableContainer() {
        context.load().json("/context/conf-site-templates.json", "/conf");
        Resource template = context.resourceResolver()
            .getResource("/conf/mysite/settings/wcm/templates/landing-page");
        assertEquals("root/container", SiteAppsSupport.findEditableContainerPath(template));
    }

    @Test
    public void editableContainerNullWithoutStructure() {
        context.load().json("/context/conf-site-templates.json", "/conf");
        Resource template = context.resourceResolver()
            .getResource("/conf/mysite/settings/wcm/templates/no-content");
        assertNull(SiteAppsSupport.findEditableContainerPath(template));
    }

    @Test
    public void editableContainerNullForNullTemplate() {
        assertNull(SiteAppsSupport.findEditableContainerPath(null));
    }

    // --- resolveComponentDefinition ---

    @Test
    public void resolvesRelativeTypeUnderApps() {
        context.build().resource("/apps/mysite/components/teaser",
            "jcr:primaryType", "cq:Component", "jcr:title", "Teaser").commit();
        Resource definition = SiteAppsSupport.resolveComponentDefinition(
            context.resourceResolver(), "mysite/components/teaser");
        assertEquals("/apps/mysite/components/teaser", definition.getPath());
    }

    @Test
    public void resolvesRelativeTypeUnderLibsWhenAbsentFromApps() {
        context.build().resource("/libs/wcm/foundation/components/text",
            "jcr:primaryType", "cq:Component").commit();
        Resource definition = SiteAppsSupport.resolveComponentDefinition(
            context.resourceResolver(), "wcm/foundation/components/text");
        assertEquals("/libs/wcm/foundation/components/text", definition.getPath());
    }

    @Test
    public void resolvesAbsoluteTypeAsIs() {
        context.build().resource("/apps/mysite/components/hero",
            "jcr:primaryType", "cq:Component").commit();
        Resource definition = SiteAppsSupport.resolveComponentDefinition(
            context.resourceResolver(), "/apps/mysite/components/hero");
        assertEquals("/apps/mysite/components/hero", definition.getPath());
    }

    @Test
    public void nullForNonComponentNode() {
        context.build().resource("/apps/mysite/components/notacomponent",
            "jcr:primaryType", "nt:unstructured").commit();
        assertNull(SiteAppsSupport.resolveComponentDefinition(
            context.resourceResolver(), "mysite/components/notacomponent"));
    }

    @Test
    public void nullForUnresolvableType() {
        assertNull(SiteAppsSupport.resolveComponentDefinition(
            context.resourceResolver(), "mysite/components/missing"));
    }

    @Test
    public void nullForBlankType() {
        assertNull(SiteAppsSupport.resolveComponentDefinition(context.resourceResolver(), ""));
        assertNull(SiteAppsSupport.resolveComponentDefinition(context.resourceResolver(), null));
    }

    // --- resolveSiteResourceType ---

    private static final String CORE_TEASER = "core/cif/components/commerce/productteaser/v1/productteaser";

    private Page landingPage(String path, String resourceType) {
        return pageWithContent(path, "jcr:primaryType", "cq:PageContent", "sling:resourceType", resourceType);
    }

    @Test
    public void resolveSiteResourceTypeReturnsProxyWhenOneSuperTypesCore() {
        Page landing = landingPage("/content/venia/us/en", "venia/components/structure/page");
        context.build().resource("/apps/venia/components/commerce/productteaser",
            "jcr:primaryType", "cq:Component", "sling:resourceSuperType", CORE_TEASER).commit();
        assertEquals("venia/components/commerce/productteaser",
            SiteAppsSupport.resolveSiteResourceType(context.resourceResolver(), landing, CORE_TEASER));
    }

    @Test
    public void resolveSiteResourceTypeFollowsMultiHopSuperTypeChain() {
        Page landing = landingPage("/content/venia/us/en", "venia/components/structure/page");
        // proxy -> intermediate (outside the components root, resolved via the /apps search path) -> core
        context.build().resource("/apps/venia/components/commerce/productteaser",
            "jcr:primaryType", "cq:Component", "sling:resourceSuperType", "venia/base/productteaser").commit();
        context.build().resource("/apps/venia/base/productteaser",
            "jcr:primaryType", "cq:Component", "sling:resourceSuperType", CORE_TEASER).commit();
        assertEquals("venia/components/commerce/productteaser",
            SiteAppsSupport.resolveSiteResourceType(context.resourceResolver(), landing, CORE_TEASER));
    }

    @Test
    public void resolveSiteResourceTypeFallsBackToCoreWhenNoProxySuperTypesIt() {
        Page landing = landingPage("/content/venia/us/en", "venia/components/structure/page");
        // a proxy exists but super-types an unrelated core component, so it must not match
        context.build().resource("/apps/venia/components/commerce/other",
            "jcr:primaryType", "cq:Component",
            "sling:resourceSuperType", "core/cif/components/commerce/product/v1/product").commit();
        assertEquals(CORE_TEASER,
            SiteAppsSupport.resolveSiteResourceType(context.resourceResolver(), landing, CORE_TEASER));
    }

    @Test
    public void resolveSiteResourceTypeFallsBackToCoreWhenAppsRootMissing() {
        Page landing = landingPage("/content/venia/us/en", "venia/components/structure/page");
        assertEquals(CORE_TEASER,
            SiteAppsSupport.resolveSiteResourceType(context.resourceResolver(), landing, CORE_TEASER));
    }

    @Test
    public void resolveSiteResourceTypeFallsBackToCoreForNullLandingPage() {
        assertEquals(CORE_TEASER,
            SiteAppsSupport.resolveSiteResourceType(context.resourceResolver(), null, CORE_TEASER));
    }

    @Test
    public void resolveSiteResourceTypeFallsBackToCoreForCoreNamespaceLandingPage() {
        Page landing = landingPage("/content/site/en", "core/cif/components/structure/page/v3/page");
        context.build().resource("/apps/venia/components/commerce/productteaser",
            "jcr:primaryType", "cq:Component", "sling:resourceSuperType", CORE_TEASER).commit();
        // landing page carries no site-specific namespace (core) -> no apps root derivable -> core returned
        assertEquals(CORE_TEASER,
            SiteAppsSupport.resolveSiteResourceType(context.resourceResolver(), landing, CORE_TEASER));
    }

    // --- toJcrValue ---

    @Test
    public void convertsScalarsToNativeTypes() throws Exception {
        assertEquals("hello", SiteAppsSupport.toJcrValue(json("\"hello\"")));
        assertEquals(Boolean.TRUE, SiteAppsSupport.toJcrValue(json("true")));
        assertEquals(Long.valueOf(42), SiteAppsSupport.toJcrValue(json("42")));
        assertEquals(Double.valueOf(3.5), SiteAppsSupport.toJcrValue(json("3.5")));
    }

    @Test
    public void jsonNullYieldsJavaNullNotTheStringNull() throws Exception {
        assertNull(SiteAppsSupport.toJcrValue(json("null")));
        assertNull(SiteAppsSupport.toJcrValue(null));
    }

    @Test
    public void arrayBecomesStringArray() throws Exception {
        Object value = SiteAppsSupport.toJcrValue(json("[\"a\", \"b\", 3]"));
        assertArrayEquals(new String[] { "a", "b", "3" }, (String[]) value);
    }

    @Test
    public void rejectsArrayWithNullOrNestedElements() {
        assertThrows(IllegalArgumentException.class,
            () -> SiteAppsSupport.toJcrValue(json("[\"a\", null]")));
        assertThrows(IllegalArgumentException.class,
            () -> SiteAppsSupport.toJcrValue(json("[{\"x\":1}]")));
    }

    @Test
    public void rejectsNestedObject() {
        assertThrows(IllegalArgumentException.class,
            () -> SiteAppsSupport.toJcrValue(json("{\"x\":1}")));
    }

    // --- requireInsidePageContent ---

    @Test
    public void acceptsNodeInsidePageContent() {
        context.build().resource("/content/site/jcr:content/root/grid",
            "jcr:primaryType", "nt:unstructured").commit();
        Resource resource = context.resourceResolver().getResource("/content/site/jcr:content/root/grid");
        SiteAppsSupport.requireInsidePageContent(resource, "path"); // must not throw
    }

    @Test
    public void rejectsJcrContentItself() {
        context.build().resource("/content/site/jcr:content",
            "jcr:primaryType", "cq:PageContent").commit();
        Resource resource = context.resourceResolver().getResource("/content/site/jcr:content");
        assertThrows(IllegalArgumentException.class,
            () -> SiteAppsSupport.requireInsidePageContent(resource, "path"));
    }

    @Test
    public void rejectsNodeOutsideAnyPageContent() {
        context.build().resource("/content/site/notpagecontent",
            "jcr:primaryType", "nt:unstructured").commit();
        Resource resource = context.resourceResolver().getResource("/content/site/notpagecontent");
        assertThrows(IllegalArgumentException.class,
            () -> SiteAppsSupport.requireInsidePageContent(resource, "path"));
    }
}
