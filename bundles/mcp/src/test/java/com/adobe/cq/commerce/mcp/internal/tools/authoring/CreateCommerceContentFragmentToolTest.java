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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.FragmentData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateCommerceContentFragmentToolTest {

    private static final String PARENT_PATH = "/content/dam/commerce";
    private static final String MODEL_PATH = "/conf/myproj/settings/dam/cfm/models/product-detail";

    @Rule
    public AemContext context = new AemContext();

    private final ObjectMapper mapper = new ObjectMapper();

    private StoreContext storeContext() {
        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(context.resourceResolver());
        return ctx;
    }

    private void seedParentAndModel() {
        context.build().resource(PARENT_PATH, "jcr:primaryType", "sling:Folder").commit();
        context.build().resource(MODEL_PATH, "jcr:primaryType", "cq:Template").commit();
    }

    /**
     * A record of the fields seeded on the canned fragment during a test, so readback can round-trip: setValue on an
     * element's FragmentData records the last value written, and getValue returns it, mirroring real persistence.
     */
    private ContentFragment cannedFragment(Resource fragmentResource, final Map<String, Object> store) {
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(fragmentResource);
        when(cf.getName()).thenReturn(fragmentResource.getName());
        when(cf.getElement(org.mockito.Mockito.anyString())).thenAnswer(invocation -> {
            final String name = (String) invocation.getArguments()[0];
            ContentElement el = mock(ContentElement.class);
            FragmentData data = mock(FragmentData.class);
            when(data.getValue()).thenAnswer(i -> store.get(name));
            org.mockito.Mockito.doAnswer(i -> {
                store.put(name, i.getArguments()[0]);
                return null;
            }).when(data).setValue(org.mockito.Mockito.any());
            when(el.getValue()).thenReturn(data);
            when(el.getName()).thenReturn(name);
            return el;
        });
        return cf;
    }

    private CreateCommerceContentFragmentTool toolFor(final ContentFragment fragment,
        final Resource[] capturedParent, final Resource[] capturedModel, final String[] capturedName,
        final String[] capturedTitle) {
        return new CreateCommerceContentFragmentTool() {
            @Override
            protected ContentFragment createFragment(ResourceResolver resolver, Resource parent, Resource model,
                String name, String title) {
                capturedParent[0] = parent;
                capturedModel[0] = model;
                capturedName[0] = name;
                capturedTitle[0] = title;
                return fragment;
            }
        };
    }

    @Test
    public void createsFragmentSeedsLinkElementAndCommits() throws Exception {
        seedParentAndModel();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/commerce/vsk-01"), store);

        Resource[] cp = new Resource[1];
        Resource[] cm = new Resource[1];
        String[] cn = new String[1];
        String[] ct = new String[1];

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        JsonNode out = toolFor(cf, cp, cm, cn, ct).call(storeContext(), args);

        // seam called with the resolved parent + model
        assertEquals(PARENT_PATH, cp[0].getPath());
        assertEquals(MODEL_PATH, cm[0].getPath());
        // linkElement seeded with the identifier, round-tripped via real readback
        assertEquals("VSK-01", store.get("sku"));
        assertTrue(out.get("seeded").toString().contains("sku"));
        assertEquals(MODEL_PATH, out.get("modelPath").asText());
        assertEquals("/content/dam/commerce/vsk-01", out.get("fragmentPath").asText());
        assertFalse(out.get("dryRun").asBoolean());
    }

    @Test
    public void seedsExtraScalarFields() throws Exception {
        seedParentAndModel();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/commerce/vsk-01"), store);

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);
        args.putObject("fields").put("headline", "Great Product").put("subtitle", "On sale");

        JsonNode out = plainTool(cf).call(storeContext(), args);

        // every fields entry seeded and round-tripped
        assertEquals("VSK-01", store.get("sku"));
        assertEquals("Great Product", store.get("headline"));
        assertEquals("On sale", store.get("subtitle"));
        String seeded = out.get("seeded").toString();
        assertTrue(seeded.contains("sku"));
        assertTrue(seeded.contains("headline"));
        assertTrue(seeded.contains("subtitle"));
    }

    @Test
    public void derivesUniqueNameAndTitleFromIdentifier() throws Exception {
        seedParentAndModel();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/commerce/vsk-01"), store);

        Resource[] cp = new Resource[1];
        Resource[] cm = new Resource[1];
        String[] cn = new String[1];
        String[] ct = new String[1];

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK 01!").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        toolFor(cf, cp, cm, cn, ct).call(storeContext(), args);

        // name sanitized from identifier (lowercase, non-alphanumeric -> '-'); title defaults to raw identifier
        assertEquals("vsk-01-", cn[0]);
        assertEquals("VSK 01!", ct[0]);
    }

    @Test
    public void honoursExplicitNameAndTitle() throws Exception {
        seedParentAndModel();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/commerce/custom"), store);

        Resource[] cp = new Resource[1];
        Resource[] cm = new Resource[1];
        String[] cn = new String[1];
        String[] ct = new String[1];

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH).put("name", "custom").put("title", "My Title");

        toolFor(cf, cp, cm, cn, ct).call(storeContext(), args);

        assertEquals("custom", cn[0]);
        assertEquals("My Title", ct[0]);
    }

    @Test
    public void dryRunCreatesNothingAndPreviewsPathAndSeeded() throws Exception {
        seedParentAndModel();
        final boolean[] seamCalled = { false };
        ResourceResolver spyResolver = org.mockito.Mockito.spy(context.resourceResolver());
        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(spyResolver);

        CreateCommerceContentFragmentTool tool = new CreateCommerceContentFragmentTool() {
            @Override
            protected ContentFragment createFragment(ResourceResolver resolver, Resource parent, Resource model,
                String name, String title) {
                seamCalled[0] = true;
                return null;
            }
        };

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH).put("dryRun", true);
        args.putObject("fields").put("headline", "Great Product");

        JsonNode out = tool.call(ctx, args);

        assertFalse(seamCalled[0]);
        verify(spyResolver, never()).commit();
        assertTrue(out.get("dryRun").asBoolean());
        assertEquals(PARENT_PATH + "/vsk-01", out.get("fragmentPath").asText());
        assertEquals(MODEL_PATH, out.get("modelPath").asText());
        String seeded = out.get("seeded").toString();
        assertTrue(seeded.contains("sku"));
        assertTrue(seeded.contains("headline"));
    }

    @Test
    public void commitsResolverOnRealCreate() throws Exception {
        seedParentAndModel();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/commerce/vsk-01"), store);

        ResourceResolver spyResolver = org.mockito.Mockito.spy(context.resourceResolver());
        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(spyResolver);

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        plainTool(cf).call(ctx, args);

        verify(spyResolver, times(1)).commit();
    }

    @Test
    public void defaultsParentPathToContentDam() throws Exception {
        // no explicit parentPath -> defaults to /content/dam (must resolve)
        context.build().resource("/content/dam", "jcr:primaryType", "sling:Folder").commit();
        context.build().resource(MODEL_PATH, "jcr:primaryType", "cq:Template").commit();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/vsk-01"), store);

        Resource[] cp = new Resource[1];
        Resource[] cm = new Resource[1];
        String[] cn = new String[1];
        String[] ct = new String[1];

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku");

        toolFor(cf, cp, cm, cn, ct).call(storeContext(), args);

        assertEquals("/content/dam", cp[0].getPath());
    }

    @Test
    public void throwsWhenParentPathNotUnderContentDam() {
        context.build().resource("/content/site/notdam", "jcr:primaryType", "sling:Folder").commit();
        context.build().resource(MODEL_PATH, "jcr:primaryType", "cq:Template").commit();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", "/content/site/notdam");

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenParentPathDoesNotResolve() {
        context.build().resource(MODEL_PATH, "jcr:primaryType", "cq:Template").commit();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", "/content/dam/does-not-exist");

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenTypeInvalid() {
        seedParentAndModel();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "widget").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenIdentifierMissing() {
        seedParentAndModel();
        ObjectNode args = mapper.createObjectNode();
        args.put("type", "product").put("modelPath", MODEL_PATH).put("linkElement", "sku").put("parentPath",
            PARENT_PATH);

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenModelPathMissing() {
        seedParentAndModel();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("linkElement", "sku").put("parentPath",
            PARENT_PATH);

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenLinkElementMissing() {
        seedParentAndModel();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH).put("parentPath",
            PARENT_PATH);

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenModelPathDoesNotResolve() {
        context.build().resource(PARENT_PATH, "jcr:primaryType", "sling:Folder").commit();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath",
            "/conf/does-not-exist/model").put("linkElement", "sku").put("parentPath", PARENT_PATH);

        // real (non-seamed) call: modelPath does not resolve -> IAE, before any create is attempted
        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void throwsWhenModelIsNotAContentFragmentModel() {
        // real seam: a resource that does not adaptTo(FragmentTemplate) -> IAE (aem-mock never adapts one)
        seedParentAndModel();
        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        assertThrows(IllegalArgumentException.class, () -> new CreateCommerceContentFragmentTool()
            .call(storeContext(), args));
    }

    @Test
    public void reportsReadbackFailureWhenSeedDoesNotRoundTrip() throws Exception {
        seedParentAndModel();
        // canned fragment whose element setValue is a no-op, so getValue never reflects the write
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(fragmentResourceAt("/content/dam/commerce/vsk-01"));
        when(cf.getName()).thenReturn("vsk-01");
        ContentElement el = mock(ContentElement.class);
        FragmentData data = mock(FragmentData.class);
        when(data.getValue()).thenReturn("STALE"); // readback never matches the seeded value
        when(el.getValue()).thenReturn(data);
        when(el.getName()).thenReturn("sku");
        when(cf.getElement(org.mockito.Mockito.anyString())).thenReturn(el);

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        // seed wrote, commit happened, but the persisted value did not round-trip -> fail closed
        assertThrows(IllegalStateException.class, () -> plainTool(cf).call(storeContext(), args));
    }

    @Test
    public void seedsMultiValueLinkElementAsSingleEntryArray() throws Exception {
        // The commerce sku (product-reference) linkElement is multi-value: a scalar identifier must be seeded as a
        // single-element String[] so it persists (this is the case live validation exposed on the Venia model).
        seedParentAndModel();
        final Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.adaptTo(Resource.class)).thenReturn(fragmentResourceAt("/content/dam/commerce/vsk-01"));
        when(cf.getName()).thenReturn("vsk-01");
        when(cf.getElement(org.mockito.Mockito.anyString())).thenAnswer(invocation -> {
            final String name = (String) invocation.getArguments()[0];
            ContentElement el = mock(ContentElement.class);
            FragmentData data = mock(FragmentData.class);
            DataType dataType = mock(DataType.class);
            when(dataType.isMultiValue()).thenReturn(true); // multi-value element (e.g. product-reference)
            when(data.getDataType()).thenReturn(dataType);
            when(data.getValue()).thenAnswer(i -> store.get(name));
            org.mockito.Mockito.doAnswer(i -> {
                store.put(name, i.getArguments()[0]);
                return null;
            }).when(data).setValue(org.mockito.Mockito.any());
            when(el.getValue()).thenReturn(data);
            when(el.getName()).thenReturn(name);
            return el;
        });

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "VSK-01").put("type", "product").put("modelPath", MODEL_PATH)
            .put("linkElement", "sku").put("parentPath", PARENT_PATH);

        JsonNode out = plainTool(cf).call(storeContext(), args);

        // seeded as a single-element String[], not a scalar String, so a multi-value element round-trips
        Object stored = store.get("sku");
        assertTrue("expected a String[] to be seeded, got " + stored, stored instanceof String[]);
        assertArrayEquals(new String[] { "VSK-01" }, (String[]) stored);
        assertTrue(out.get("seeded").toString().contains("sku"));
    }

    @Test
    public void categoryTypeIsAccepted() throws Exception {
        seedParentAndModel();
        Map<String, Object> store = new HashMap<String, Object>();
        ContentFragment cf = cannedFragment(fragmentResourceAt("/content/dam/commerce/cat-uid"), store);

        ObjectNode args = mapper.createObjectNode();
        args.put("identifier", "MjU=").put("type", "category").put("modelPath", MODEL_PATH)
            .put("linkElement", "categoryUid").put("parentPath", PARENT_PATH);

        JsonNode out = plainTool(cf).call(storeContext(), args);
        assertEquals("MjU=", store.get("categoryUid"));
        assertTrue(out.get("seeded").toString().contains("categoryUid"));
    }

    @Test
    public void writesContentReturnsTrue() {
        assertTrue(new CreateCommerceContentFragmentTool().writesContent());
    }

    // --- helpers -------------------------------------------------------------

    private CreateCommerceContentFragmentTool plainTool(final ContentFragment fragment) {
        return new CreateCommerceContentFragmentTool() {
            @Override
            protected ContentFragment createFragment(ResourceResolver resolver, Resource parent, Resource model,
                String name, String title) {
                return fragment;
            }
        };
    }

    private Resource fragmentResourceAt(String path) {
        context.build().resource(path, "jcr:primaryType", "dam:Asset").commit();
        return context.resourceResolver().getResource(path);
    }
}
