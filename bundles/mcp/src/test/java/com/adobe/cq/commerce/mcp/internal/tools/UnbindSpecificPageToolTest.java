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
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
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

public class UnbindSpecificPageToolTest {
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

    private void createStructurePage(String path) {
        context.create().page(path);
        context.resourceResolver().getResource(path + "/jcr:content")
            .adaptTo(ModifiableValueMap.class)
            .put("sling:resourceType", "core/cif/components/structure/page/v3/page");
    }

    @Test
    public void clearsAllBoundFieldsAndReportsWhatWasCleared() throws Exception {
        String path = "/content/site/bound-page";
        createStructurePage(path);
        ModifiableValueMap properties = context.resourceResolver().getResource(path + "/jcr:content")
            .adaptTo(ModifiableValueMap.class);
        properties.put("selectorFilter", new String[] { "MjA=|venia-tops" });
        properties.put("includesSubCategories", true);
        context.resourceResolver().commit();

        JsonNode out = new UnbindSpecificPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + path + "\"}"));

        assertEquals(path, out.get("path").asText());
        assertTrue(out.get("updated").asBoolean());

        Set<String> cleared = new HashSet<>();
        out.get("cleared").forEach(node -> cleared.add(node.asText()));
        assertEquals(new HashSet<>(Arrays.asList("selectorFilter", "includesSubCategories")), cleared);

        Resource content = context.resourceResolver().getResource(path + "/jcr:content");
        assertNull(content.getValueMap().get("selectorFilter", String[].class));
        assertNull(content.getValueMap().get("selectorFilterType", String.class));
        assertNull(content.getValueMap().get("includesSubCategories", Boolean.class));
        assertNull(content.getValueMap().get("useForCategories", String[].class));
    }

    @Test
    public void clearsAllFourFieldsWhenAllPresent() throws Exception {
        String path = "/content/site/fully-bound-page";
        createStructurePage(path);
        ModifiableValueMap properties = context.resourceResolver().getResource(path + "/jcr:content")
            .adaptTo(ModifiableValueMap.class);
        properties.put("selectorFilter", new String[] { "jillian-top" });
        properties.put("selectorFilterType", "uidAndUrlPath");
        properties.put("includesSubCategories", true);
        properties.put("useForCategories", new String[] { "MjA=|venia-tops" });
        context.resourceResolver().commit();

        JsonNode out = new UnbindSpecificPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + path + "\"}"));

        assertTrue(out.get("updated").asBoolean());
        Set<String> cleared = new HashSet<>();
        out.get("cleared").forEach(node -> cleared.add(node.asText()));
        assertEquals(new HashSet<>(Arrays.asList(
            "selectorFilter", "selectorFilterType", "includesSubCategories", "useForCategories")), cleared);

        Resource content = context.resourceResolver().getResource(path + "/jcr:content");
        assertNull(content.getValueMap().get("selectorFilter", String[].class));
        assertNull(content.getValueMap().get("selectorFilterType", String.class));
        assertNull(content.getValueMap().get("includesSubCategories", Boolean.class));
        assertNull(content.getValueMap().get("useForCategories", String[].class));
    }

    @Test
    public void isIdempotentOnAlreadyUnboundPage() throws Exception {
        String path = "/content/site/unbound-page";
        createStructurePage(path);
        context.resourceResolver().commit();

        JsonNode out = new UnbindSpecificPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"" + path + "\"}"));

        assertEquals(path, out.get("path").asText());
        assertFalse(out.get("updated").asBoolean());
        assertEquals(0, out.get("cleared").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingPath() throws Exception {
        new UnbindSpecificPageTool().call(ctxForResolver(), mapper.readTree("{}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonCifPage() throws Exception {
        // A page whose jcr:content is not a CIF structure page type must fail closed.
        context.create().page("/content/site/plainpage");
        context.resourceResolver().commit();

        new UnbindSpecificPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/plainpage\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPageResource() throws Exception {
        // A component resource (not a cq:Page at all) must fail closed -- it does not adapt to Page.
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        new UnbindSpecificPageTool().call(ctxForResolver(), mapper.readTree(
            "{\"path\":\"/content/site/jcr:content/root/teaser\"}"));
    }

    @Test
    public void writesContentIsTrue() {
        assertTrue(new UnbindSpecificPageTool().writesContent());
    }

    @Test
    public void toolNameIsExpected() {
        assertEquals("unbind_specific_page", new UnbindSpecificPageTool().name());
        assertFalse(new UnbindSpecificPageTool().description().isEmpty());
    }
}
