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
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

public class ValidateContentBindingsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsPerIdentifierResolutionFlagsForProductsAndCategories() {
        Set<String> resolvingSkus = new HashSet<>(Arrays.asList("24-MB01"));
        Set<String> resolvingUids = new HashSet<>(Arrays.asList("cat1"));

        StoreContext ctx = mock(StoreContext.class);
        ValidateContentBindingsTool tool = new ValidateContentBindingsTool() {
            @Override
            protected boolean productResolves(StoreContext c, String sku) {
                return resolvingSkus.contains(sku);
            }

            @Override
            protected boolean categoryResolves(StoreContext c, String uid) {
                return resolvingUids.contains(uid);
            }
        };

        ArrayNode products = mapper.createArrayNode().add("24-MB01").add("discontinued-sku");
        ArrayNode categories = mapper.createArrayNode().add("cat1").add("removed-cat");
        ObjectNode args = mapper.createObjectNode();
        args.set("products", products);
        args.set("categories", categories);

        JsonNode out = tool.call(ctx, args);

        assertEquals("validate_content_bindings", tool.name());

        JsonNode productResults = out.get("products");
        assertEquals(2, productResults.size());
        assertEquals("24-MB01", productResults.get(0).get("sku").asText());
        assertEquals(true, productResults.get(0).get("resolves").asBoolean());
        assertEquals("discontinued-sku", productResults.get(1).get("sku").asText());
        assertEquals(false, productResults.get(1).get("resolves").asBoolean());

        JsonNode categoryResults = out.get("categories");
        assertEquals(2, categoryResults.size());
        assertEquals("cat1", categoryResults.get(0).get("uid").asText());
        assertEquals(true, categoryResults.get(0).get("resolves").asBoolean());
        assertEquals("removed-cat", categoryResults.get(1).get("uid").asText());
        assertEquals(false, categoryResults.get(1).get("resolves").asBoolean());
    }

    @Test
    public void supportsProductsOnly() {
        StoreContext ctx = mock(StoreContext.class);
        ValidateContentBindingsTool tool = new ValidateContentBindingsTool() {
            @Override
            protected boolean productResolves(StoreContext c, String sku) {
                return true;
            }
        };

        JsonNode args = mapper.createObjectNode().set("products", mapper.createArrayNode().add("24-MB01"));
        JsonNode out = tool.call(ctx, args);

        assertEquals(1, out.get("products").size());
        assertEquals(true, out.get("products").get(0).get("resolves").asBoolean());
        assertEquals(0, out.get("categories").size());
    }

    @Test
    public void supportsCategoriesOnly() {
        StoreContext ctx = mock(StoreContext.class);
        ValidateContentBindingsTool tool = new ValidateContentBindingsTool() {
            @Override
            protected boolean categoryResolves(StoreContext c, String uid) {
                return false;
            }
        };

        JsonNode args = mapper.createObjectNode().set("categories", mapper.createArrayNode().add("cat1"));
        JsonNode out = tool.call(ctx, args);

        assertEquals(0, out.get("products").size());
        assertEquals(1, out.get("categories").size());
        assertEquals(false, out.get("categories").get(0).get("resolves").asBoolean());
    }

    @Test
    public void ignoresNullArrayElements() {
        StoreContext ctx = mock(StoreContext.class);
        ValidateContentBindingsTool tool = new ValidateContentBindingsTool() {
            @Override
            protected boolean productResolves(StoreContext c, String sku) {
                return true;
            }
        };

        ArrayNode products = mapper.createArrayNode();
        products.add("24-MB01");
        products.addNull();
        JsonNode args = mapper.createObjectNode().set("products", products);

        JsonNode out = tool.call(ctx, args);

        assertEquals(1, out.get("products").size());
        assertEquals("24-MB01", out.get("products").get(0).get("sku").asText());
    }

    @Test
    public void bothArraysAbsentThrows() {
        StoreContext ctx = mock(StoreContext.class);
        ValidateContentBindingsTool tool = new ValidateContentBindingsTool();
        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx, mapper.createObjectNode()));
    }

    @Test
    public void bothArraysEmptyThrows() {
        StoreContext ctx = mock(StoreContext.class);
        ValidateContentBindingsTool tool = new ValidateContentBindingsTool();
        ObjectNode args = mapper.createObjectNode();
        args.set("products", mapper.createArrayNode());
        args.set("categories", mapper.createArrayNode());
        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx, args));
    }
}
