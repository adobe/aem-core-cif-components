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

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.Breadcrumb;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResolveCategoryDetailsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private Breadcrumb breadcrumb(String uid, String name, int level, String urlPath) {
        Breadcrumb crumb = mock(Breadcrumb.class);
        when(crumb.getCategoryUid()).thenReturn(new ID(uid));
        when(crumb.getCategoryName()).thenReturn(name);
        when(crumb.getCategoryLevel()).thenReturn(level);
        when(crumb.getCategoryUrlPath()).thenReturn(urlPath);
        return crumb;
    }

    @Test
    public void returnsFlattenedCategoryDetailsWithOrderedBreadcrumbs() {
        Breadcrumb level2 = breadcrumb("cat2", "Tops", 2, "men/tops");
        Breadcrumb level1 = breadcrumb("cat1", "Men", 1, "men");

        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getUid()).thenReturn(new ID("cat3"));
        when(category.getName()).thenReturn("Shirts");
        when(category.getUrlPath()).thenReturn("men/tops/shirts");
        when(category.getBreadcrumbs()).thenReturn(Arrays.asList(level2, level1));

        StoreContext ctx = mock(StoreContext.class);
        ResolveCategoryDetailsTool tool = new ResolveCategoryDetailsTool() {
            @Override
            protected CategoryInterface fetch(StoreContext c, String identifier, String categoryIdType) {
                return category;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("uid", "cat3"));
        assertEquals("resolve_category_details", tool.name());
        assertEquals("cat3", out.get("uid").asText());
        assertEquals("Shirts", out.get("name").asText());
        assertEquals("men/tops/shirts", out.get("urlPath").asText());

        JsonNode breadcrumbs = out.get("breadcrumbs");
        assertEquals(2, breadcrumbs.size());
        assertEquals("cat1", breadcrumbs.get(0).get("uid").asText());
        assertEquals("Men", breadcrumbs.get(0).get("name").asText());
        assertEquals(1, breadcrumbs.get(0).get("level").asInt());
        assertEquals("men", breadcrumbs.get(0).get("urlPath").asText());
        assertEquals("cat2", breadcrumbs.get(1).get("uid").asText());
        assertEquals("Tops", breadcrumbs.get(1).get("name").asText());
        assertEquals(2, breadcrumbs.get(1).get("level").asInt());
        assertEquals("men/tops", breadcrumbs.get(1).get("urlPath").asText());
    }

    @Test
    public void resolvesIdentifierAndCategoryIdTypeFromUrlPath() {
        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getUid()).thenReturn(new ID("cat3"));

        String[] captured = new String[2];
        StoreContext ctx = mock(StoreContext.class);
        ResolveCategoryDetailsTool tool = new ResolveCategoryDetailsTool() {
            @Override
            protected CategoryInterface fetch(StoreContext c, String identifier, String categoryIdType) {
                captured[0] = identifier;
                captured[1] = categoryIdType;
                return category;
            }
        };

        tool.call(ctx, mapper.createObjectNode().put("uid", "cat3").put("urlPath", "men/tops/shirts"));

        assertEquals("men/tops/shirts", captured[0]);
        assertEquals("urlPath", captured[1]);
    }

    @Test
    public void resolvesIdentifierAndDefaultCategoryIdTypeFromUidOnly() {
        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getUid()).thenReturn(new ID("cat3"));

        String[] captured = new String[2];
        StoreContext ctx = mock(StoreContext.class);
        ResolveCategoryDetailsTool tool = new ResolveCategoryDetailsTool() {
            @Override
            protected CategoryInterface fetch(StoreContext c, String identifier, String categoryIdType) {
                captured[0] = identifier;
                captured[1] = categoryIdType;
                return category;
            }
        };

        tool.call(ctx, mapper.createObjectNode().put("uid", "cat3"));

        assertEquals("cat3", captured[0]);
        assertEquals(null, captured[1]);
    }

    @Test
    public void returnsNotFoundResultWhenFetchYieldsNull() {
        StoreContext ctx = mock(StoreContext.class);
        ResolveCategoryDetailsTool tool = new ResolveCategoryDetailsTool() {
            @Override
            protected CategoryInterface fetch(StoreContext c, String identifier, String categoryIdType) {
                return null;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("uid", "does-not-exist"));

        assertEquals("does-not-exist", out.get("uid").asText());
        assertFalse(out.get("resolves").asBoolean());
        assertEquals(2, out.size());
    }

    @Test
    public void missingUidThrows() {
        StoreContext ctx = mock(StoreContext.class);
        ResolveCategoryDetailsTool tool = new ResolveCategoryDetailsTool();
        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx, mapper.createObjectNode()));
    }
}
