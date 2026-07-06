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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListPageTemplatesToolTest {

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

    private JsonNode findByPath(JsonNode templates, String path) {
        for (JsonNode template : templates) {
            if (path.equals(template.path("path").asText())) {
                return template;
            }
        }
        return null;
    }

    @Test
    public void listsTemplatesForExplicitConfPath() throws Exception {
        context.load().json("/context/conf-site-templates.json", "/conf");

        JsonNode out = new ListPageTemplatesTool().call(ctxForResolver(),
            mapper.readTree("{\"confPath\":\"/conf/mysite\"}"));

        assertEquals("/conf/mysite", out.get("confPath").asText());
        JsonNode templates = out.get("templates");
        assertEquals(2, templates.size()); // no-content has no jcr:content and is skipped

        JsonNode landing = findByPath(templates, "/conf/mysite/settings/wcm/templates/landing-page");
        assertEquals("Landing page", landing.get("title").asText());
        assertEquals("Generic landing page", landing.get("description").asText());
        assertEquals("enabled", landing.get("status").asText());
        assertEquals(1, landing.get("allowedPaths").size());
        assertEquals("/content/mysite(/.*)?", landing.get("allowedPaths").get(0).asText());
        assertEquals("root/container", landing.get("editableContainerPath").asText());
        assertNull(landing.get("commerceKind")); // generic marketing template: no commerce signal

        JsonNode disabled = findByPath(templates, "/conf/mysite/settings/wcm/templates/disabled-page");
        assertEquals("disabled", disabled.get("status").asText());
    }

    @Test
    public void reportsCommerceKindForCommerceTemplate() throws Exception {
        context.load().json("/context/conf-site-templates.json", "/conf");
        // A template whose initial grid can't be read at all falls back to the jcr:title convention.
        context.build().resource("/conf/mysite/settings/wcm/templates/product-page",
            "jcr:primaryType", "cq:Template")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent", "jcr:title", "Product page")
            .commit();

        JsonNode out = new ListPageTemplatesTool().call(ctxForResolver(),
            mapper.readTree("{\"confPath\":\"/conf/mysite\"}"));

        JsonNode product = findByPath(out.get("templates"), "/conf/mysite/settings/wcm/templates/product-page");
        assertEquals("product", product.get("commerceKind").asText());
    }

    @Test
    public void derivesConfPathFromLandingPageTemplate() throws Exception {
        context.load().json("/context/conf-site-templates.json", "/conf");
        context.build().resource("/content/mysite/en", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent",
                "cq:template", "/conf/mysite/settings/wcm/templates/landing-page")
            .commit();
        StoreContext ctx = ctxForResolver();
        Page landingPage = context.resourceResolver().getResource("/content/mysite/en").adaptTo(Page.class);
        when(ctx.getLandingPage()).thenReturn(landingPage);

        JsonNode out = new ListPageTemplatesTool().call(ctx, mapper.readTree("{}"));

        assertEquals("/conf/mysite", out.get("confPath").asText());
        assertEquals(2, out.get("templates").size());
    }

    @Test
    public void scansAllConfRootsWithoutSiteSignal() throws Exception {
        context.load().json("/context/conf-site-templates.json", "/conf");

        JsonNode out = new ListPageTemplatesTool().call(ctxForResolver(), mapper.readTree("{}"));

        assertTrue(out.get("confPath").isNull());
        assertEquals(2, out.get("templates").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsConfPathNotUnderConf() throws Exception {
        new ListPageTemplatesTool().call(ctxForResolver(), mapper.readTree("{\"confPath\":\"/content/mysite\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonExistentConfPath() throws Exception {
        new ListPageTemplatesTool().call(ctxForResolver(), mapper.readTree("{\"confPath\":\"/conf/nothere\"}"));
    }

    @Test
    public void isAuthoringOnlyReadTool() {
        ListPageTemplatesTool tool = new ListPageTemplatesTool();
        assertTrue(tool.authoringOnly());
        assertTrue(!tool.writesContent());
        assertEquals("list_page_templates", tool.name());
    }
}
