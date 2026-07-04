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
package com.adobe.cq.commerce.mcp.internal;

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

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
}
