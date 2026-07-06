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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateContentPageToolTest {

    private static final String TEMPLATE = "/conf/mysite/settings/wcm/templates/landing-page";

    @Rule
    public final AemContext context = new AemContext();
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String[]> createCalls = new ArrayList<String[]>();

    // Seam-backed tool: records the create arguments and stages a stub page, so the test does not depend on a
    // real PageManager template copy (see the seam contract on PageCreationSupport).
    private final CreateContentPageTool tool = new CreateContentPageTool() {
        @Override
        protected String createPage(ResourceResolver resolver, String parentPath, String name, String templatePath,
            String title) {
            createCalls.add(new String[] { parentPath, name, templatePath, title });
            String pagePath = parentPath + "/" + name;
            context.build().resource(pagePath, "jcr:primaryType", "cq:Page")
                .resource("jcr:content", "jcr:primaryType", "cq:PageContent", "jcr:title", title);
            return pagePath;
        }
    };

    @Before
    public void setUp() {
        context.load().json("/context/conf-site-templates.json", "/conf");
        context.build().resource("/content/mysite", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent");
        context.build().resource("/content/othersite", "jcr:primaryType", "cq:Page")
            .resource("jcr:content", "jcr:primaryType", "cq:PageContent");
    }

    private StoreContext ctxForResolver() {
        StoreContext ctx = mock(StoreContext.class);
        SlingHttpServletRequest req = mock(SlingHttpServletRequest.class);
        when(req.getResourceResolver()).thenReturn(context.resourceResolver());
        when(ctx.getRequest()).thenReturn(req);
        return ctx;
    }

    private JsonNode call(String json) throws Exception {
        return tool.call(ctxForResolver(), mapper.readTree(json));
    }

    @Test
    public void createsPageFromTemplate() throws Exception {
        JsonNode out = call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\",\"template\":\"" + TEMPLATE + "\"}");

        assertEquals("/content/mysite/Summer", out.get("pagePath").asText());
        assertEquals(TEMPLATE, out.get("template").asText());
        assertEquals("root/container", out.get("editableContainerPath").asText());
        assertEquals("/content/mysite/Summer/jcr:content/root/container", out.get("containerPath").asText());
        assertFalse(out.get("dryRun").asBoolean());
        assertEquals(1, createCalls.size());
        assertEquals("Summer", createCalls.get(0)[1]);
        assertEquals(TEMPLATE, createCalls.get(0)[2]);
        assertEquals("Summer", createCalls.get(0)[3]);
    }

    @Test
    public void derivesUniqueNameOnCollision() throws Exception {
        context.build().resource("/content/mysite/Summer");

        JsonNode out = call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\",\"template\":\"" + TEMPLATE + "\"}");

        assertEquals("/content/mysite/Summer0", out.get("pagePath").asText());
    }

    @Test
    public void usesExplicitName() throws Exception {
        JsonNode out = call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\",\"template\":\"" + TEMPLATE
            + "\",\"name\":\"summer-sale\"}");

        assertEquals("/content/mysite/summer-sale", out.get("pagePath").asText());
    }

    @Test
    public void dryRunCreatesNothing() throws Exception {
        JsonNode out = call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\",\"template\":\"" + TEMPLATE
            + "\",\"dryRun\":true}");

        assertTrue(out.get("dryRun").asBoolean());
        assertEquals("/content/mysite/Summer", out.get("pagePath").asText());
        assertEquals("/content/mysite/Summer/jcr:content/root/container", out.get("containerPath").asText());
        assertTrue(createCalls.isEmpty());
        assertNull(context.resourceResolver().getResource("/content/mysite/Summer"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankTitle() throws Exception {
        call("{\"parent\":\"/content/mysite\",\"title\":\"\",\"template\":\"" + TEMPLATE + "\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingParent() throws Exception {
        call("{\"parent\":\"/content/nothere\",\"title\":\"Summer\",\"template\":\"" + TEMPLATE + "\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTemplateNotUnderConf() throws Exception {
        call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\",\"template\":\"/apps/mysite/template\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownTemplate() throws Exception {
        call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\","
            + "\"template\":\"/conf/mysite/settings/wcm/templates/nothere\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDisabledTemplate() throws Exception {
        call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\","
            + "\"template\":\"/conf/mysite/settings/wcm/templates/disabled-page\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTemplateWithoutContent() throws Exception {
        call("{\"parent\":\"/content/mysite\",\"title\":\"Summer\","
            + "\"template\":\"/conf/mysite/settings/wcm/templates/no-content\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentOutsideTemplateAllowedPaths() throws Exception {
        call("{\"parent\":\"/content/othersite\",\"title\":\"Summer\",\"template\":\"" + TEMPLATE + "\"}");
    }

    @Test
    public void isAuthoringOnlyWriteTool() {
        CreateContentPageTool plain = new CreateContentPageTool();
        assertTrue(plain.writesContent());
        assertTrue(plain.authoringOnly());
        assertEquals("create_content_page", plain.name());
    }
}
