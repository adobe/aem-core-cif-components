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
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetCategoryBreadcrumbsToolTest {
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
    public void returnsBreadcrumbsOrderedByLevelAscending() throws Exception {
        // Out-of-order input: level 2 first, then level 1.
        Breadcrumb level2 = breadcrumb("cat2", "Tops", 2, "men/tops");
        Breadcrumb level1 = breadcrumb("cat1", "Men", 1, "men");

        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getUid()).thenReturn(new ID("cat3"));
        when(category.getBreadcrumbs()).thenReturn(Arrays.asList(level2, level1));

        StoreContext ctx = mock(StoreContext.class);
        GetCategoryBreadcrumbsTool tool = new GetCategoryBreadcrumbsTool() {
            @Override
            protected CategoryInterface fetch(StoreContext c, String uid, String urlPath) {
                return category;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("uid", "cat3"));
        assertEquals("get_category_breadcrumbs", tool.name());
        assertEquals("cat3", out.get("uid").asText());

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
    public void missingUidAndUrlPathThrows() {
        StoreContext ctx = mock(StoreContext.class);
        GetCategoryBreadcrumbsTool tool = new GetCategoryBreadcrumbsTool();
        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx, mapper.createObjectNode()));
    }
}
