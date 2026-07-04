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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.FragmentData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetCommerceContentFragmentToolTest {

    @Rule
    public AemContext context = new AemContext();

    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext storeContext(Resource resource) {
        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(context.resourceResolver());
        return ctx;
    }

    private ContentElement scalarElement(String name, String value) {
        ContentElement element = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getValue()).thenReturn(value);
        when(element.getName()).thenReturn(name);
        when(element.getValue()).thenReturn(data);
        return element;
    }

    private ContentElement multiValueElement(String name, String[] values) {
        ContentElement element = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getValue()).thenReturn(values);
        when(element.getName()).thenReturn(name);
        when(element.getValue()).thenReturn(data);
        return element;
    }

    @Test
    public void returnsFieldsForMatchedProductContentFragment() throws Exception {
        context.load().json("/context/commerce-cf.json", "/content");
        Resource cfResource = context.resourceResolver().getResource("/content/dam/vsk01-details");

        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(cfResource);
        Iterator<ContentElement> elements = Arrays.asList(
            scalarElement("linkElement", "VSK01"),
            scalarElement("headline", "Bellona Details"),
            multiValueElement("tags", new String[] { "summer", "sale" })).iterator();
        when(cf.getElements()).thenReturn(elements);

        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool() {
            @Override
            protected ContentFragment resolveContentFragment(StoreContext ctx, String type, String identifier,
                String contentFragmentModel, String linkElement) {
                return cf;
            }
        };

        JsonNode args = mapper.createObjectNode().put("identifier", "VSK01").put("type", "product")
            .put("contentFragmentModel", "/conf/venia/settings/dam/cfm/models/product").put("linkElement",
                "linkElement");
        JsonNode out = tool.call(storeContext(cfResource), args);

        assertEquals("get_commerce_content_fragment", tool.name());
        assertEquals("VSK01", out.get("identifier").asText());
        assertEquals("product", out.get("type").asText());
        assertEquals("/conf/venia/settings/dam/cfm/models/product", out.get("modelPath").asText());
        assertEquals("/content/dam/vsk01-details", out.get("fragmentPath").asText());
        assertFalse(out.has("resolves"));

        JsonNode fields = out.get("fields");
        assertEquals("VSK01", fields.get("linkElement").asText());
        assertEquals("Bellona Details", fields.get("headline").asText());
        assertTrue(fields.get("tags").isArray());
        assertEquals(2, fields.get("tags").size());
        assertEquals("summer", fields.get("tags").get(0).asText());
        assertEquals("sale", fields.get("tags").get(1).asText());
    }

    @Test
    public void resolvesModelPathFromCqModelWhenContentFragmentModelArgAbsent() throws Exception {
        context.load().json("/context/commerce-cf.json", "/content");
        Resource cfResource = context.resourceResolver().getResource("/content/dam/vsk01-details");

        ContentElement linkElementField = scalarElement("linkElement", "VSK01");
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(cfResource);
        when(cf.getElements()).thenReturn(Collections.singletonList(linkElementField).iterator());

        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool() {
            @Override
            protected ContentFragment resolveContentFragment(StoreContext ctx, String type, String identifier,
                String contentFragmentModel, String linkElement) {
                return cf;
            }
        };

        JsonNode args = mapper.createObjectNode().put("identifier", "VSK01").put("type", "product");
        JsonNode out = tool.call(storeContext(cfResource), args);

        assertEquals("/conf/venia/settings/dam/cfm/models/product", out.get("modelPath").asText());
    }

    @Test
    public void returnsResolvesFalseWhenNoContentFragmentMatches() throws Exception {
        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool() {
            @Override
            protected ContentFragment resolveContentFragment(StoreContext ctx, String type, String identifier,
                String contentFragmentModel, String linkElement) {
                return null;
            }
        };

        JsonNode args = mapper.createObjectNode().put("identifier", "no-such-sku").put("type", "product");
        JsonNode out = tool.call(storeContext(null), args);

        assertEquals("no-such-sku", out.get("identifier").asText());
        assertEquals("product", out.get("type").asText());
        assertFalse(out.get("resolves").asBoolean());
        assertFalse(out.has("fields"));
    }

    @Test
    public void throwsWhenIdentifierMissing() {
        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool();
        JsonNode args = mapper.createObjectNode().put("type", "product");
        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(null), args));
    }

    @Test
    public void throwsWhenIdentifierBlank() {
        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool();
        JsonNode args = mapper.createObjectNode().put("identifier", "   ").put("type", "product");
        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(null), args));
    }

    @Test
    public void throwsWhenTypeInvalid() {
        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool();
        JsonNode args = mapper.createObjectNode().put("identifier", "VSK01").put("type", "bogus");
        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(null), args));
    }

    @Test
    public void throwsWhenTypeMissing() {
        GetCommerceContentFragmentTool tool = new GetCommerceContentFragmentTool();
        JsonNode args = mapper.createObjectNode().put("identifier", "VSK01");
        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(null), args));
    }
}
