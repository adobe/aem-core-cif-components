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

import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowseCategoriesToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsCategoryTree() throws Exception {
        CategoryTree cat = mock(CategoryTree.class);
        when(cat.getName()).thenReturn("Tops");
        when(cat.getUrlPath()).thenReturn("tops");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        BrowseCategoriesTool tool = new BrowseCategoriesTool() {
            @Override
            protected CategoryInterface fetch(StoreContext c, String uid) {
                return cat;
            }
        };
        JsonNode out = tool.call(ctx, mapper.createObjectNode());
        assertEquals("Tops", out.get("category").get("name").asText());
        assertEquals("browse_categories", tool.name());
    }
}
