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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.FragmentData;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommerceWriteSupportTest {

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "core/cif/components/commerce/productteaser/v1/productteaser");

    @Rule
    public final AemContext context = new AemContext();

    @Test
    public void resolvesResourceOfAllowedType() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        ResourceResolver resolver = context.resourceResolver();
        Resource resolved = CommerceWriteSupport.resolveComponent(resolver, "path",
            "/content/site/jcr:content/root/teaser", ALLOWED_TYPES);

        assertNotNull(resolved);
        assertEquals("/content/site/jcr:content/root/teaser", resolved.getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDisallowedResourceType() {
        context.build().resource("/content/site/jcr:content/root/text",
            "sling:resourceType", "core/wcm/components/text/v2/text").commit();

        CommerceWriteSupport.resolveComponent(context.resourceResolver(), "path",
            "/content/site/jcr:content/root/text", ALLOWED_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathNotUnderContent() {
        CommerceWriteSupport.resolveComponent(context.resourceResolver(), "path", "/etc/somewhere", ALLOWED_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankPath() {
        CommerceWriteSupport.resolveComponent(context.resourceResolver(), "path", "   ", ALLOWED_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullPath() {
        CommerceWriteSupport.resolveComponent(context.resourceResolver(), "path", null, ALLOWED_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingResource() {
        CommerceWriteSupport.resolveComponent(context.resourceResolver(), "path", "/content/does/not/exist",
            ALLOWED_TYPES);
    }

    @Test
    public void mutableMapAdaptsModifiableResource() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        Resource resource = context.resourceResolver().getResource("/content/site/jcr:content/root/teaser");

        ModifiableValueMap map = CommerceWriteSupport.mutableMap(resource, "path");

        assertNotNull(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mutableMapRejectsNonAdaptableResource() {
        // A synthetic non-JCR resource (no ModifiableValueMap adapter available) must fail closed.
        Resource resource = new org.apache.sling.api.resource.SyntheticResource(context.resourceResolver(),
            "/content/synthetic", "sling:nonexistent") {
            @Override
            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }
        };

        CommerceWriteSupport.mutableMap(resource, "path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveComponentRejectsEmptyAllowedTypesList() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        CommerceWriteSupport.resolveComponent(context.resourceResolver(), "path",
            "/content/site/jcr:content/root/teaser", Collections.<String>emptyList());
    }

    private static final List<String> ALLOWED_PAGE_TYPES = Arrays.asList(
        "core/cif/components/structure/page/v3/page");

    @Test
    public void resolvePageContentResolvesPageOfAllowedType() throws Exception {
        context.create().page("/content/site/structpage");
        context.resourceResolver().getResource("/content/site/structpage/jcr:content")
            .adaptTo(ModifiableValueMap.class)
            .put("sling:resourceType", "core/cif/components/structure/page/v3/page");
        context.resourceResolver().commit();

        Resource content = CommerceWriteSupport.resolvePageContent(context.resourceResolver(), "path",
            "/content/site/structpage", ALLOWED_PAGE_TYPES);

        assertNotNull(content);
        assertEquals("/content/site/structpage/jcr:content", content.getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolvePageContentRejectsNonPageResource() {
        // A component resource (not a cq:Page) must fail closed -- it does not adapt to Page.
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();

        CommerceWriteSupport.resolvePageContent(context.resourceResolver(), "path",
            "/content/site/jcr:content/root/teaser", ALLOWED_PAGE_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolvePageContentRejectsWrongContentType() {
        context.create().page("/content/site/plainpage");
        // jcr:content keeps aem-mock's default resource type (not a CIF structure page type).

        CommerceWriteSupport.resolvePageContent(context.resourceResolver(), "path",
            "/content/site/plainpage", ALLOWED_PAGE_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolvePageContentRejectsMissingResource() {
        CommerceWriteSupport.resolvePageContent(context.resourceResolver(), "path", "/content/does/not/exist",
            ALLOWED_PAGE_TYPES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolvePageContentRejectsBlankPath() {
        CommerceWriteSupport.resolvePageContent(context.resourceResolver(), "path", "  ", ALLOWED_PAGE_TYPES);
    }

    @Test
    public void putOrRemoveWritesNonBlankValue() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        ModifiableValueMap map = CommerceWriteSupport.mutableMap(
            context.resourceResolver().getResource("/content/site/jcr:content/root/teaser"), "path");

        CommerceWriteSupport.putOrRemove(map, "cta", "add-to-cart");

        assertEquals("add-to-cart", map.get("cta", String.class));
    }

    @Test
    public void putOrRemoveRemovesBlankValue() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser",
            "cta", "add-to-cart").commit();
        ModifiableValueMap map = CommerceWriteSupport.mutableMap(
            context.resourceResolver().getResource("/content/site/jcr:content/root/teaser"), "path");

        // Whitespace-only is never a meaningful commerce value -- it clears the property (isBlank semantics),
        // same as null/empty.
        CommerceWriteSupport.putOrRemove(map, "cta", "  ");

        assertEquals(null, map.get("cta", String.class));
    }

    @Test
    public void putOrRemoveArrayWritesNonEmptyList() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        ModifiableValueMap map = CommerceWriteSupport.mutableMap(
            context.resourceResolver().getResource("/content/site/jcr:content/root/teaser"), "path");

        CommerceWriteSupport.putOrRemoveArray(map, "product", Arrays.asList("MJ01", "MJ02"));

        assertArrayEquals(new String[] { "MJ01", "MJ02" }, map.get("product", String[].class));
    }

    @Test
    public void putOrRemoveArrayRemovesEmptyList() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser",
            "product", new String[] { "MJ01" }).commit();
        ModifiableValueMap map = CommerceWriteSupport.mutableMap(
            context.resourceResolver().getResource("/content/site/jcr:content/root/teaser"), "path");

        CommerceWriteSupport.putOrRemoveArray(map, "product", Collections.<String>emptyList());

        assertNull(map.get("product", String[].class));
    }

    @Test
    public void putOrRemoveArrayRemovesNullList() {
        context.build().resource("/content/site/jcr:content/root/teaser",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser",
            "product", new String[] { "MJ01" }).commit();
        ModifiableValueMap map = CommerceWriteSupport.mutableMap(
            context.resourceResolver().getResource("/content/site/jcr:content/root/teaser"), "path");

        CommerceWriteSupport.putOrRemoveArray(map, "product", null);

        assertNull(map.get("product", String[].class));
    }

    @Test
    public void writeCompositeCreatesChildNodesWithProps() throws Exception {
        context.build().resource("/content/site/jcr:content/root/list",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/list");

        Map<String, Object> item0 = new LinkedHashMap<String, Object>();
        item0.put("categoryId", "MjA=");
        Map<String, Object> item1 = new LinkedHashMap<String, Object>();
        item1.put("categoryId", "Mjk=");
        item1.put("asset", "/content/dam/x.jpg");

        CommerceWriteSupport.writeComposite(resolver, parent, "items", Arrays.asList(item0, item1));
        resolver.commit();

        Resource items = resolver.getResource("/content/site/jcr:content/root/list/items");
        assertNotNull(items);
        assertEquals("MjA=", items.getChild("item0").getValueMap().get("categoryId", String.class));
        assertEquals("Mjk=", items.getChild("item1").getValueMap().get("categoryId", String.class));
        assertEquals("/content/dam/x.jpg", items.getChild("item1").getValueMap().get("asset", String.class));
    }

    @Test
    public void writeCompositeClearsContainerWhenItemsEmpty() throws Exception {
        context.build().resource("/content/site/jcr:content/root/list2",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/list2");

        Map<String, Object> item0 = new LinkedHashMap<String, Object>();
        item0.put("categoryId", "MjA=");
        CommerceWriteSupport.writeComposite(resolver, parent, "items", Arrays.asList(item0));
        resolver.commit();
        assertNotNull(resolver.getResource("/content/site/jcr:content/root/list2/items"));

        CommerceWriteSupport.writeComposite(resolver, parent, "items", Collections.<Map<String, Object>>emptyList());
        resolver.commit();

        assertNull(resolver.getResource("/content/site/jcr:content/root/list2/items"));
    }

    @Test
    public void writeCompositeClearsContainerWhenItemsNull() throws Exception {
        context.build().resource("/content/site/jcr:content/root/list3",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/list3");

        Map<String, Object> item0 = new LinkedHashMap<String, Object>();
        item0.put("categoryId", "MjA=");
        CommerceWriteSupport.writeComposite(resolver, parent, "items", Arrays.asList(item0));
        resolver.commit();

        CommerceWriteSupport.writeComposite(resolver, parent, "items", null);
        resolver.commit();

        assertNull(resolver.getResource("/content/site/jcr:content/root/list3/items"));
    }

    @Test
    public void createChildCreatesUniquelyNamedChildWithProps() throws Exception {
        context.build().resource("/content/site/jcr:content/root/grid",
            "jcr:primaryType", "nt:unstructured").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/grid");

        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser");
        props.put("selection", "MJ01");

        Resource created = CommerceWriteSupport.createChild(resolver, parent, "productteaser", props);
        resolver.commit();

        assertNotNull(created);
        assertEquals("productteaser", created.getName());
        assertEquals("/content/site/jcr:content/root/grid/productteaser", created.getPath());
        assertEquals("nt:unstructured", created.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("core/cif/components/commerce/productteaser/v1/productteaser",
            created.getValueMap().get("sling:resourceType", String.class));
        assertEquals("MJ01", created.getValueMap().get("selection", String.class));
    }

    @Test
    public void createChildYieldsDistinctNameOnSecondCallWithSameBase() throws Exception {
        context.build().resource("/content/site/jcr:content/root/grid2",
            "jcr:primaryType", "nt:unstructured").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/grid2");

        Map<String, Object> props1 = new LinkedHashMap<String, Object>();
        props1.put("selection", "MJ01");
        Resource first = CommerceWriteSupport.createChild(resolver, parent, "productteaser", props1);
        resolver.commit();

        Map<String, Object> props2 = new LinkedHashMap<String, Object>();
        props2.put("selection", "MJ02");
        Resource second = CommerceWriteSupport.createChild(resolver, parent, "productteaser", props2);
        resolver.commit();

        assertEquals("productteaser", first.getName());
        assertNotEquals(first.getName(), second.getName());
        assertNotEquals(first.getPath(), second.getPath());
        assertEquals("MJ02", second.getValueMap().get("selection", String.class));
    }

    @Test
    public void createChildDoesNotCommit() throws Exception {
        context.build().resource("/content/site/jcr:content/root/grid3",
            "jcr:primaryType", "nt:unstructured").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/grid3");

        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("selection", "MJ01");
        CommerceWriteSupport.createChild(resolver, parent, "productteaser", props);

        // createChild must leave the commit to the caller -- the resolver still has pending,
        // uncommitted changes after the call returns.
        assertTrue(resolver.hasChanges());
    }

    @Test
    public void resolveContainerResolvesWritableNonPageResource() {
        context.build().resource("/content/site/jcr:content/root/grid", "jcr:primaryType", "nt:unstructured")
            .commit();

        Resource container = CommerceWriteSupport.resolveContainer(context.resourceResolver(), "parentPath",
            "/content/site/jcr:content/root/grid");

        assertNotNull(container);
        assertEquals("/content/site/jcr:content/root/grid", container.getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContainerRejectsBlankPath() {
        CommerceWriteSupport.resolveContainer(context.resourceResolver(), "parentPath", "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContainerRejectsNullPath() {
        CommerceWriteSupport.resolveContainer(context.resourceResolver(), "parentPath", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContainerRejectsPathNotUnderContent() {
        context.build().resource("/apps/site/grid", "jcr:primaryType", "nt:unstructured").commit();

        CommerceWriteSupport.resolveContainer(context.resourceResolver(), "parentPath", "/apps/site/grid");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContainerRejectsMissingResource() {
        CommerceWriteSupport.resolveContainer(context.resourceResolver(), "parentPath", "/content/does/not/exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContainerRejectsCqPage() throws Exception {
        context.create().page("/content/site/apage");

        CommerceWriteSupport.resolveContainer(context.resourceResolver(), "parentPath", "/content/site/apage");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContainerRejectsNonModifiableResource() {
        // A synthetic non-JCR resource (no ModifiableValueMap adapter available) must fail closed, even though it
        // is under /content and is not a Page.
        Resource resource = new org.apache.sling.api.resource.SyntheticResource(context.resourceResolver(),
            "/content/synthetic", "sling:nonexistent") {
            @Override
            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }
        };
        ResourceResolver spyResolver = org.mockito.Mockito.spy(context.resourceResolver());
        org.mockito.Mockito.doReturn(resource).when(spyResolver).getResource("/content/synthetic");

        CommerceWriteSupport.resolveContainer(spyResolver, "parentPath", "/content/synthetic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContentFragmentRejectsBlankPath() {
        CommerceWriteSupport.resolveContentFragment(context.resourceResolver(), "fragmentPath", "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContentFragmentRejectsNullPath() {
        CommerceWriteSupport.resolveContentFragment(context.resourceResolver(), "fragmentPath", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContentFragmentRejectsPathNotUnderDam() {
        context.build().resource("/content/site/notdam", "jcr:primaryType", "nt:unstructured").commit();

        CommerceWriteSupport.resolveContentFragment(context.resourceResolver(), "fragmentPath", "/content/site/notdam");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContentFragmentRejectsMissingResource() {
        CommerceWriteSupport.resolveContentFragment(context.resourceResolver(), "fragmentPath",
            "/content/dam/does/not/exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveContentFragmentRejectsNonContentFragmentAsset() {
        // aem-mock does not register a ContentFragment adapter, so a plain DAM resource never adapts -- this
        // exercises the same "non-CF asset -> null adaptTo" fail-closed gate that a real, non-CF dam:Asset would
        // hit in production (the ContentFragment-adapt behavior itself is covered at the tool level via a seam,
        // see UpdateCommerceContentFragmentFieldToolTest).
        context.build().resource("/content/dam/notacf", "jcr:primaryType", "dam:Asset").commit();

        CommerceWriteSupport.resolveContentFragment(context.resourceResolver(), "fragmentPath", "/content/dam/notacf");
    }

    @Test
    public void writeCompositeReplacesPriorChildrenOnRewrite() throws Exception {
        context.build().resource("/content/site/jcr:content/root/list4",
            "sling:resourceType", "core/cif/components/commerce/productteaser/v1/productteaser").commit();
        ResourceResolver resolver = context.resourceResolver();
        Resource parent = resolver.getResource("/content/site/jcr:content/root/list4");

        Map<String, Object> a = new LinkedHashMap<String, Object>();
        a.put("categoryId", "AAA=");
        Map<String, Object> b = new LinkedHashMap<String, Object>();
        b.put("categoryId", "BBB=");
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("categoryId", "CCC=");
        CommerceWriteSupport.writeComposite(resolver, parent, "items", Arrays.asList(a, b, c));
        resolver.commit();

        Map<String, Object> d = new LinkedHashMap<String, Object>();
        d.put("categoryId", "DDD=");
        CommerceWriteSupport.writeComposite(resolver, parent, "items", Arrays.asList(d));
        resolver.commit();

        Resource items = resolver.getResource("/content/site/jcr:content/root/list4/items");
        assertNotNull(items);
        assertEquals("DDD=", items.getChild("item0").getValueMap().get("categoryId", String.class));
        assertNull(items.getChild("item1"));
        assertNull(items.getChild("item2"));
    }

    // --- applyElementValue / elementValueRoundTrips (content-fragment element writes) --------------------------

    private ContentElement elementWith(FragmentData data) {
        ContentElement element = mock(ContentElement.class);
        when(element.getValue()).thenReturn(data);
        when(element.getName()).thenReturn("field");
        return element;
    }

    private FragmentData dataOfArity(boolean multiValue) {
        FragmentData data = mock(FragmentData.class);
        DataType dataType = mock(DataType.class);
        when(dataType.isMultiValue()).thenReturn(multiValue);
        when(data.getDataType()).thenReturn(dataType);
        return data;
    }

    @Test
    public void applyElementValueWritesScalarToSingleValueElement() throws Exception {
        FragmentData data = dataOfArity(false);
        ContentElement element = elementWith(data);

        CommerceWriteSupport.applyElementValue(element, null, false, new String[] { "hello" });

        verify(data).setValue("hello");
        verify(element).setValue(data);
    }

    @Test
    public void applyElementValueWritesArrayToMultiValueElement() throws Exception {
        FragmentData data = dataOfArity(true);
        ContentElement element = elementWith(data);

        CommerceWriteSupport.applyElementValue(element, null, false, new String[] { "a", "b" });

        verify(data).setValue(new String[] { "a", "b" });
        verify(element).setValue(data);
    }

    @Test
    public void applyElementValueWrapsScalarAsArrayForMultiValueElement() throws Exception {
        FragmentData data = dataOfArity(true);
        ContentElement element = elementWith(data);

        CommerceWriteSupport.applyElementValue(element, null, false, new String[] { "VT01" });

        // a multi-value element receives a String[], never a bare String -- this is the product-reference sku case
        verify(data).setValue(new String[] { "VT01" });
    }

    @Test
    public void applyElementValueRejectsMultipleValuesForSingleValueElement() {
        FragmentData data = dataOfArity(false);
        ContentElement element = elementWith(data);

        assertThrows(IllegalArgumentException.class,
            () -> CommerceWriteSupport.applyElementValue(element, null, false, new String[] { "a", "b" }));
    }

    @Test
    public void applyElementValueTranslatesContentFragmentExceptionToIllegalArgument() throws Exception {
        FragmentData data = dataOfArity(false);
        doThrow(new ContentFragmentException("nope")).when(data).setValue(any());
        ContentElement element = elementWith(data);

        assertThrows(IllegalArgumentException.class,
            () -> CommerceWriteSupport.applyElementValue(element, null, false, new String[] { "x" }));
    }

    @Test
    public void applyElementValueWritesToNamedVariation() throws Exception {
        FragmentData vdata = dataOfArity(false);
        ContentVariation variation = mock(ContentVariation.class);
        when(variation.getValue()).thenReturn(vdata);
        ContentElement element = mock(ContentElement.class);
        when(element.getName()).thenReturn("field");

        CommerceWriteSupport.applyElementValue(element, variation, true, new String[] { "v" });

        verify(vdata).setValue("v");
        verify(variation).setValue(vdata);
        verify(element, org.mockito.Mockito.never()).setValue(any(FragmentData.class));
    }

    @Test
    public void elementValueRoundTripsTrueWhenScalarMatches() {
        FragmentData data = dataOfArity(false);
        when(data.getValue()).thenReturn("hello");
        ContentElement element = elementWith(data);

        assertTrue(CommerceWriteSupport.elementValueRoundTrips(element, null, false, new String[] { "hello" }));
    }

    @Test
    public void elementValueRoundTripsFalseWhenScalarMismatches() {
        FragmentData data = dataOfArity(false);
        when(data.getValue()).thenReturn("stale");
        ContentElement element = elementWith(data);

        assertFalse(CommerceWriteSupport.elementValueRoundTrips(element, null, false, new String[] { "hello" }));
    }

    @Test
    public void elementValueRoundTripsTrueWhenArrayMatches() {
        FragmentData data = dataOfArity(true);
        when(data.getValue()).thenReturn(new String[] { "a", "b" });
        ContentElement element = elementWith(data);

        assertTrue(CommerceWriteSupport.elementValueRoundTrips(element, null, false, new String[] { "a", "b" }));
    }

    @Test
    public void elementValueRoundTripsFalseWhenDataNull() {
        ContentElement element = elementWith(null);

        assertFalse(CommerceWriteSupport.elementValueRoundTrips(element, null, false, new String[] { "x" }));
    }

    @Test
    public void elementValueRoundTripsFalseWhenArrayMismatches() {
        FragmentData data = dataOfArity(true);
        when(data.getValue()).thenReturn(new String[] { "a" });
        ContentElement element = elementWith(data);

        assertFalse(CommerceWriteSupport.elementValueRoundTrips(element, null, false, new String[] { "a", "b" }));
    }

    @Test
    public void elementValueRoundTripsHandlesNullEntryWithoutNpe() {
        // A single written null entry compares equal only to a null read-back, and never throws on expected[0].
        FragmentData nullData = dataOfArity(false);
        when(nullData.getValue()).thenReturn(null);
        assertTrue(CommerceWriteSupport.elementValueRoundTrips(elementWith(nullData), null, false,
            new String[] { null }));

        FragmentData nonNullData = dataOfArity(false);
        when(nonNullData.getValue()).thenReturn("something");
        assertFalse(CommerceWriteSupport.elementValueRoundTrips(elementWith(nonNullData), null, false,
            new String[] { null }));
    }

    @Test
    public void elementValueRoundTripsReadsBackNamedVariation() {
        FragmentData vdata = dataOfArity(false);
        when(vdata.getValue()).thenReturn("v");
        ContentVariation variation = mock(ContentVariation.class);
        when(variation.getValue()).thenReturn(vdata);
        ContentElement element = mock(ContentElement.class);

        assertTrue(CommerceWriteSupport.elementValueRoundTrips(element, variation, true, new String[] { "v" }));
        // the base element value is never consulted on the variation path
        verify(element, org.mockito.Mockito.never()).getValue();
    }

    @Test
    public void elementValueRoundTripsFalseWhenVariationNull() {
        ContentElement element = mock(ContentElement.class);

        assertFalse(CommerceWriteSupport.elementValueRoundTrips(element, null, true, new String[] { "v" }));
    }
}
