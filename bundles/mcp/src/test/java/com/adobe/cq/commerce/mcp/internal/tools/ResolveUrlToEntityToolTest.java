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

import com.adobe.cq.commerce.magento.graphql.EntityUrl;
import com.adobe.cq.commerce.magento.graphql.UrlRewriteEntityTypeEnum;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ResolveUrlToEntityToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void mapsResolvedEntityUrl() {
        EntityUrl entityUrl = new EntityUrl().setType(UrlRewriteEntityTypeEnum.PRODUCT)
            .setId(42)
            .setEntityUid(new ID("abc"))
            .setCanonicalUrl("/catalog/product/view/id/42")
            .setRelativeUrl("my-product.html")
            .setRedirectCode(0);

        ResolveUrlToEntityTool tool = new ResolveUrlToEntityTool() {
            @Override
            protected EntityUrl fetch(StoreContext ctx, String url) {
                assertEquals("my-product.html", url);
                return entityUrl;
            }
        };

        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = mapper.createObjectNode().put("url", "my-product.html");

        JsonNode out = tool.call(ctx, args);

        assertEquals("resolve_url_to_entity", tool.name());
        assertEquals("my-product.html", out.get("url").asText());
        assertEquals("PRODUCT", out.get("type").asText());
        assertEquals(42, out.get("id").asInt());
        assertEquals("abc", out.get("uid").asText());
        assertEquals("/catalog/product/view/id/42", out.get("canonicalUrl").asText());
        assertEquals("my-product.html", out.get("relativeUrl").asText());
        assertEquals(0, out.get("redirectCode").asInt());
    }

    @Test
    public void returnsResolvesFalseWhenNotFound() {
        ResolveUrlToEntityTool tool = new ResolveUrlToEntityTool() {
            @Override
            protected EntityUrl fetch(StoreContext ctx, String url) {
                return null;
            }
        };

        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = mapper.createObjectNode().put("url", "does-not-exist.html");

        JsonNode out = tool.call(ctx, args);

        assertEquals("does-not-exist.html", out.get("url").asText());
        assertFalse(out.get("resolves").asBoolean());
        assertTrue(out.path("type").isMissingNode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenUrlMissing() {
        ResolveUrlToEntityTool tool = new ResolveUrlToEntityTool();
        StoreContext ctx = mock(StoreContext.class);
        tool.call(ctx, mapper.createObjectNode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenUrlBlank() {
        ResolveUrlToEntityTool tool = new ResolveUrlToEntityTool();
        StoreContext ctx = mock(StoreContext.class);
        ObjectNode args = mapper.createObjectNode().put("url", "   ");
        tool.call(ctx, args);
    }
}
