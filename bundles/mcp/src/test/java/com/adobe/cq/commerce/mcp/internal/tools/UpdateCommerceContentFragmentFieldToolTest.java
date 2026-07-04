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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.FragmentData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateCommerceContentFragmentFieldToolTest {

    private static final String FRAGMENT_PATH = "/content/dam/vsk01-details";

    @Rule
    public AemContext context = new AemContext();

    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext storeContext() {
        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(context.resourceResolver());
        return ctx;
    }

    private Resource fragmentResource() {
        context.build().resource(FRAGMENT_PATH, "jcr:primaryType", "dam:Asset").commit();
        return context.resourceResolver().getResource(FRAGMENT_PATH);
    }

    private UpdateCommerceContentFragmentFieldTool toolFor(ContentFragment fragment) {
        return new UpdateCommerceContentFragmentFieldTool() {
            @Override
            protected ContentFragment resolveFragment(ResourceResolver resolver, String fragmentPath) {
                return fragment;
            }
        };
    }

    @Test
    public void writesPlainTextFieldOnMaster() throws Exception {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getContentType()).thenReturn("text/plain");
        when(data.isTypeSupported(String.class)).thenReturn(true);
        when(el.getValue()).thenReturn(data);
        when(cf.getElement("headline")).thenReturn(el);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "headline")
            .put("value", "New Headline");
        JsonNode out = toolFor(cf).call(storeContext(), args);

        verify(data).setValue("New Headline");
        verify(el).setValue(data);
        verify(el, never()).setContent(anyString(), anyString());
        assertEquals(FRAGMENT_PATH, out.get("fragmentPath").asText());
        assertEquals("headline", out.get("elementName").asText());
        assertEquals("master", out.get("variation").asText());
        assertTrue(out.get("updated").asBoolean());
    }

    @Test
    public void writesRichtextFieldViaSetContent() throws Exception {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getContentType()).thenReturn("text/html");
        when(el.getValue()).thenReturn(data);
        when(cf.getElement("body")).thenReturn(el);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "body")
            .put("value", "<p>Hello</p>");
        JsonNode out = toolFor(cf).call(storeContext(), args);

        verify(el).setContent("<p>Hello</p>", "text/html");
        verify(data, never()).setValue(any());
        assertTrue(out.get("updated").asBoolean());
    }

    @Test
    public void writesMultiValueFieldAsStringArray() throws Exception {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getContentType()).thenReturn("text/plain");
        when(data.isTypeSupported(String[].class)).thenReturn(true);
        when(el.getValue()).thenReturn(data);
        when(cf.getElement("tags")).thenReturn(el);

        ObjectNode args = mapper.createObjectNode();
        args.put("fragmentPath", FRAGMENT_PATH).put("elementName", "tags");
        args.putArray("value").add("summer").add("sale");
        JsonNode out = toolFor(cf).call(storeContext(), args);

        verify(data).setValue(new String[] { "summer", "sale" });
        verify(el).setValue(data);
        assertTrue(out.get("updated").asBoolean());
    }

    @Test
    public void routesToNamedVariationWhenSpecified() throws Exception {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        ContentVariation variation = mock(ContentVariation.class);
        FragmentData vdata = mock(FragmentData.class);
        when(vdata.getContentType()).thenReturn("text/plain");
        when(vdata.isTypeSupported(String.class)).thenReturn(true);
        when(variation.getValue()).thenReturn(vdata);
        when(el.getVariation("summer-promo")).thenReturn(variation);
        when(cf.getElement("headline")).thenReturn(el);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "headline")
            .put("value", "Summer Headline").put("variation", "summer-promo");
        JsonNode out = toolFor(cf).call(storeContext(), args);

        verify(vdata).setValue("Summer Headline");
        verify(variation).setValue(vdata);
        verify(el, never()).setValue(any(FragmentData.class));
        assertEquals("summer-promo", out.get("variation").asText());
    }

    @Test
    public void routesRichtextVariationToSetContent() throws Exception {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        ContentVariation variation = mock(ContentVariation.class);
        FragmentData vdata = mock(FragmentData.class);
        when(vdata.getContentType()).thenReturn("text/html");
        when(variation.getValue()).thenReturn(vdata);
        when(el.getVariation("summer-promo")).thenReturn(variation);
        when(cf.getElement("body")).thenReturn(el);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "body")
            .put("value", "<p>Summer</p>").put("variation", "summer-promo");
        toolFor(cf).call(storeContext(), args);

        verify(variation).setContent("<p>Summer</p>", "text/html");
    }

    @Test
    public void throwsWhenVariationUnknown() {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        when(el.getVariation("no-such-variation")).thenReturn(null);
        when(cf.getElement("headline")).thenReturn(el);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "headline")
            .put("value", "x").put("variation", "no-such-variation");

        assertThrows(IllegalArgumentException.class, () -> toolFor(cf).call(storeContext(), args));
    }

    @Test
    public void throwsWhenElementUnknown() {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        when(cf.getElement("no-such-element")).thenReturn(null);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName",
            "no-such-element").put("value", "x");

        assertThrows(IllegalArgumentException.class, () -> toolFor(cf).call(storeContext(), args));
    }

    @Test
    public void throwsWhenFragmentPathNotUnderDam() {
        UpdateCommerceContentFragmentFieldTool tool = new UpdateCommerceContentFragmentFieldTool();
        JsonNode args = mapper.createObjectNode().put("fragmentPath", "/content/site/notdam").put("elementName",
            "headline").put("value", "x");

        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(), args));
    }

    @Test
    public void throwsWhenNotAContentFragment() {
        context.build().resource("/content/dam/plainasset", "jcr:primaryType", "dam:Asset").commit();
        UpdateCommerceContentFragmentFieldTool tool = new UpdateCommerceContentFragmentFieldTool();
        JsonNode args = mapper.createObjectNode().put("fragmentPath", "/content/dam/plainasset").put("elementName",
            "headline").put("value", "x");

        // Real (non-seamed) resolveFragment: aem-mock never adapts a plain resource to ContentFragment, so this
        // also proves the fail-closed CommerceWriteSupport.resolveContentFragment gate is wired into the tool.
        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(), args));
    }

    @Test
    public void throwsWhenFragmentPathMissing() {
        UpdateCommerceContentFragmentFieldTool tool = new UpdateCommerceContentFragmentFieldTool();
        JsonNode args = mapper.createObjectNode().put("elementName", "headline").put("value", "x");

        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(), args));
    }

    @Test
    public void throwsWhenElementNameMissing() {
        UpdateCommerceContentFragmentFieldTool tool = new UpdateCommerceContentFragmentFieldTool();
        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("value", "x");

        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(), args));
    }

    @Test
    public void throwsWhenValueMissing() {
        UpdateCommerceContentFragmentFieldTool tool = new UpdateCommerceContentFragmentFieldTool();
        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "headline");

        assertThrows(IllegalArgumentException.class, () -> tool.call(storeContext(), args));
    }

    @Test
    public void writesContentReturnsTrue() {
        UpdateCommerceContentFragmentFieldTool tool = new UpdateCommerceContentFragmentFieldTool();
        assertTrue(tool.writesContent());
    }

    @Test
    public void commitsResolverAfterWrite() throws Exception {
        Resource resource = fragmentResource();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(resource);
        ContentElement el = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getContentType()).thenReturn("text/plain");
        when(data.isTypeSupported(String.class)).thenReturn(true);
        when(el.getValue()).thenReturn(data);
        when(cf.getElement("headline")).thenReturn(el);

        ResourceResolver spyResolver = org.mockito.Mockito.spy(context.resourceResolver());
        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(spyResolver);

        JsonNode args = mapper.createObjectNode().put("fragmentPath", FRAGMENT_PATH).put("elementName", "headline")
            .put("value", "New Headline");
        toolFor(cf).call(ctx, args);

        verify(spyResolver, times(1)).commit();
    }
}
