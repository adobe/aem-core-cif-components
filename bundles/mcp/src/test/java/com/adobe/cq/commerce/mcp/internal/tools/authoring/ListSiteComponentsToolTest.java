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
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListSiteComponentsToolTest {

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

    private void buildSiteComponents() {
        context.build()
            .resource("/apps/mysite/components/teaser",
                "jcr:primaryType", "cq:Component",
                "jcr:title", "Teaser",
                "componentGroup", "MySite - Content",
                "sling:resourceSuperType", "core/wcm/components/teaser/v2/teaser")
            .resource("/apps/mysite/components/commerce",
                "jcr:primaryType", "sling:Folder")
            .resource("/apps/mysite/components/commerce/productteaser",
                "jcr:primaryType", "cq:Component",
                "jcr:title", "Product Teaser",
                "componentGroup", "MySite - Commerce",
                "sling:resourceSuperType", "core/cif/components/commerce/productteaser/v1/productteaser")
            .resource("/apps/mysite/components/page",
                "jcr:primaryType", "cq:Component",
                "jcr:title", "Page",
                "componentGroup", ".hidden",
                "sling:resourceSuperType", "core/cif/components/structure/page/v3/page")
            .commit();
    }

    private JsonNode findByResourceType(JsonNode components, String resourceType) {
        for (JsonNode component : components) {
            if (resourceType.equals(component.path("resourceType").asText())) {
                return component;
            }
        }
        return null;
    }

    @Test
    public void listsComponentsForExplicitAppsPathRecursively() throws Exception {
        buildSiteComponents();

        JsonNode out = new ListSiteComponentsTool().call(ctxForResolver(),
            mapper.readTree("{\"appsPath\":\"/apps/mysite/components\"}"));

        assertEquals("/apps/mysite/components", out.get("appsPath").asText());
        JsonNode components = out.get("components");
        assertEquals(2, components.size()); // hidden page component skipped; folder recursed into

        JsonNode teaser = findByResourceType(components, "mysite/components/teaser");
        assertEquals("Teaser", teaser.get("title").asText());
        assertEquals("MySite - Content", teaser.get("group").asText());
        assertEquals("core/wcm/components/teaser/v2/teaser", teaser.get("resourceSuperType").asText());

        JsonNode productTeaser = findByResourceType(components, "mysite/components/commerce/productteaser");
        assertEquals("MySite - Commerce", productTeaser.get("group").asText());
    }

    @Test
    public void includeHiddenListsHiddenComponents() throws Exception {
        buildSiteComponents();

        JsonNode out = new ListSiteComponentsTool().call(ctxForResolver(),
            mapper.readTree("{\"appsPath\":\"/apps/mysite/components\",\"includeHidden\":true}"));

        assertEquals(3, out.get("components").size());
        JsonNode page = findByResourceType(out.get("components"), "mysite/components/page");
        assertEquals(".hidden", page.get("group").asText());
    }

    @Test
    public void groupFilterListsOnlyThatGroup() throws Exception {
        buildSiteComponents();

        JsonNode out = new ListSiteComponentsTool().call(ctxForResolver(),
            mapper.readTree("{\"appsPath\":\"/apps/mysite/components\",\"group\":\"MySite - Commerce\"}"));

        assertEquals(1, out.get("components").size());
        assertEquals("mysite/components/commerce/productteaser",
            out.get("components").get(0).get("resourceType").asText());
    }

    @Test
    public void derivesAppsPathFromLandingPageResourceType() throws Exception {
        buildSiteComponents();
        context.build().resource("/content/mysite/en", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent",
                "sling:resourceType", "mysite/components/page")
            .commit();
        StoreContext ctx = ctxForResolver();
        Page landingPage = context.resourceResolver().getResource("/content/mysite/en").adaptTo(Page.class);
        when(ctx.getLandingPage()).thenReturn(landingPage);

        JsonNode out = new ListSiteComponentsTool().call(ctx, mapper.readTree("{}"));

        assertEquals("/apps/mysite/components", out.get("appsPath").asText());
        assertEquals(2, out.get("components").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWhenNoAppsPathDerivable() throws Exception {
        new ListSiteComponentsTool().call(ctxForResolver(), mapper.readTree("{}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAppsPathNotUnderApps() throws Exception {
        new ListSiteComponentsTool().call(ctxForResolver(),
            mapper.readTree("{\"appsPath\":\"/libs/mysite/components\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonExistentAppsPath() throws Exception {
        new ListSiteComponentsTool().call(ctxForResolver(),
            mapper.readTree("{\"appsPath\":\"/apps/nothere/components\"}"));
    }

    @Test
    public void isAuthoringOnlyReadTool() {
        ListSiteComponentsTool tool = new ListSiteComponentsTool();
        assertTrue(tool.authoringOnly());
        assertTrue(!tool.writesContent());
        assertEquals("list_site_components", tool.name());
    }
}
